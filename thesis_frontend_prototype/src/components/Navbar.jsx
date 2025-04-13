import { Link, useLocation } from "react-router-dom";

export default function Navbar() {
  const location = useLocation();

  return (
    <nav className="bg-gray-800 text-white py-4 px-6">
      <ul className="flex items-center">
        <li className="mr-6">
          <Link
            to="/dashboard"
            className={`hover:text-gray-300 ${location.pathname === "/dashboard" ? "font-bold underline" : ""
              }`}
          >
            Dashboard
          </Link>
        </li>
        <li className="mr-6">
          <Link
            to="/courses"
            className={`hover:text-gray-300 ${location.pathname === "/courses" ? "font-bold underline" : ""
              }`}
          >
            Courses
          </Link>
        </li>

        <li>
          <Link
            to="/pods"
            className={`hover:text-gray-300 ${location.pathname === "/pods" ? "font-bold underline" : ""
              }`}
          >
            Pod Management
          </Link>
        </li>

        {/* Logout aligned to top-right corner */}
        <li className="ml-auto">
          <Link
            to="/"
            className="bg-red-500 hover:bg-red-600 px-3 py-1 rounded-md"
          >
            Logout
          </Link>
        </li>
      </ul>
    </nav>
  );
}
