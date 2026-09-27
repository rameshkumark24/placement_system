import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { startKeepAlive, wakeBackend } from "./api";
import "./styles.css";

// Start waking the backend as soon as the page opens, before the user interacts with anything.
void wakeBackend();
startKeepAlive();

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
