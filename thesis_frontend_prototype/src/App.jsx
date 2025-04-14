import { Toaster } from "react-hot-toast";
import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";

import LoginPage from "./pages/LoginPage";
import Dashboard from "./pages/Dashboard";
import PodManagement from "./pages/PodManagement";
import CourseManagement from "./pages/CourseManagement";
import Navbar from "./components/Navbar";

function Layout() {
  return (
    <>
      <Navbar />
      <div className="container mx-auto mt-4">
        <Outlet />
      </div>
    </>
  );
}



function App() {
  return (
    <>
      <BrowserRouter>

        <Routes>
          <Route path="/" element={<LoginPage />} />

          {/* Protected routes with Navbar */}
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/pods" element={<PodManagement />} />
            <Route path="/courses" element={<CourseManagement />} />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" />
    </>

  );
}

export default App;
