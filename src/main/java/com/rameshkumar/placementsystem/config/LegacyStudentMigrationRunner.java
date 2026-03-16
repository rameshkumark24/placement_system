package com.rameshkumar.placementsystem.config;

import com.rameshkumar.placementsystem.entity.User;
import com.rameshkumar.placementsystem.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LegacyStudentMigrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LegacyStudentMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LegacyStudentMigrationRunner(JdbcTemplate jdbcTemplate,
                                        UserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureApplicationStudentForeignKey();

        if (!hasLegacyStudentColumns()) {
            logger.info("Legacy student migration skipped because legacy columns are not present");
            return;
        }

        backfillStudentUserLinks();
        remapLegacyApplicationStudentIds();
    }

    private void backfillStudentUserLinks() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, name, email, password from student where user_id is null"
        );

        if (rows.isEmpty()) {
            logger.info("No legacy student rows found for user backfill");
            return;
        }

        for (Map<String, Object> row : rows) {
            Long studentId = ((Number) row.get("id")).longValue();
            String name = (String) row.get("name");
            String email = (String) row.get("email");
            String password = (String) row.get("password");

            if (email == null || email.isBlank()) {
                logger.warn("Skipping legacy student {} because email is missing", studentId);
                continue;
            }

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> createLegacyUser(name, email, password));

            Long linkedStudentId = jdbcTemplate.query(
                    "select id from student where user_id = ?",
                    ps -> ps.setLong(1, user.getId()),
                    rs -> rs.next() ? rs.getLong("id") : null
            );

            if (linkedStudentId != null && !linkedStudentId.equals(studentId)) {
                logger.warn("Skipping legacy student {} because user {} is already linked to student {}", studentId, user.getId(), linkedStudentId);
                continue;
            }

            jdbcTemplate.update("update student set user_id = ? where id = ?", user.getId(), studentId);
            logger.info("Linked legacy student {} to user {}", studentId, user.getId());
        }
    }

    private void remapLegacyApplicationStudentIds() {
        int updatedRows = jdbcTemplate.update(
                """
                update application a
                join student s on s.user_id = a.student_id
                set a.student_id = s.id
                where a.student_id <> s.id
                """
        );

        if (updatedRows > 0) {
            logger.info("Remapped {} legacy application rows from users.id to student.id", updatedRows);
        } else {
            logger.info("No legacy application rows required remapping");
        }
    }

    private void ensureApplicationStudentForeignKey() {
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(
                """
                select constraint_name, referenced_table_name
                from information_schema.key_column_usage
                where table_schema = database()
                  and table_name = 'application'
                  and column_name = 'student_id'
                  and referenced_table_name is not null
                """
        );

        for (Map<String, Object> foreignKey : foreignKeys) {
            String constraintName = (String) foreignKey.get("constraint_name");
            String referencedTableName = (String) foreignKey.get("referenced_table_name");

            if ("student".equalsIgnoreCase(referencedTableName)) {
                logger.info("Application foreign key already points to student table");
                return;
            }

            logger.warn("Repairing application.student_id foreign key from {} to student", referencedTableName);
            jdbcTemplate.execute("alter table application drop foreign key " + constraintName);
        }

        jdbcTemplate.execute(
                """
                alter table application
                add constraint fk_application_student
                foreign key (student_id) references student(id)
                """
        );
        logger.info("Application foreign key repaired to reference student(id)");
    }

    private User createLegacyUser(String name, String email, String password) {
        User user = new User();
        user.setName(name != null && !name.isBlank() ? name : "Legacy Student");
        user.setEmail(email);
        user.setPassword(encodeIfNeeded(password));
        user.setRole("STUDENT");
        User savedUser = userRepository.save(user);
        logger.info("Created legacy user {} for migrated student email {}", savedUser.getId(), email);
        return savedUser;
    }

    private String encodeIfNeeded(String password) {
        if (password == null || password.isBlank()) {
            return passwordEncoder.encode("ChangeMe@123");
        }
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return password;
        }
        return passwordEncoder.encode(password);
    }

    private boolean hasLegacyStudentColumns() {
        Integer emailColumnCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = 'student'
                  and column_name = 'email'
                """,
                Integer.class
        );
        return emailColumnCount != null && emailColumnCount > 0;
    }
}
