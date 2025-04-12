import { Link } from "react-router-dom";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Button } from "../components/ui/button";

export default function Dashboard() {
  // Sample dummy data
  const courses = [
    { id: 1, name: "Kubernetes 101", pods: 3, students: 12 },
    { id: 2, name: "DevOps Fundamentals", pods: 2, students: 9 },
  ];

  return (
    <div className="space-y-6 p-4">
      <h1 className="text-2xl font-bold">Welcome back, Professor 👨‍🏫</h1>

      <Card>
        <CardTitle className="mb-2">Your Courses</CardTitle>
        <CardContent className="grid md:grid-cols-2 gap-4">
          {courses.map((course) => (
            <div key={course.id} className="border rounded p-4 shadow-sm bg-gray-50">
              <h2 className="font-semibold text-lg">{course.name}</h2>
              <p>Pods: {course.pods}</p>
              <p>Students: {course.students}</p>
              <Link
                to="/pods"
                className="inline-block mt-2 text-blue-600 underline"
              >
                Manage Pods
              </Link>
            </div>
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardTitle className="mb-2">Quick Actions</CardTitle>
        <CardContent className="flex gap-4 flex-wrap">
          <Link to="/courses">
            <Button>➕ Create New Course</Button>
          </Link>
          <Link to="/pods">
            <Button>➕ Add Pod</Button>
          </Link>
          <Button disabled>📊 Monitor Students (Coming soon)</Button>
        </CardContent>
      </Card>
    </div>
  );
}
