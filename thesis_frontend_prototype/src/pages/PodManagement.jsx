import { useState } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import toast from "react-hot-toast";

export default function PodManagement() {
  const [pods, setPods] = useState([
    { id: 1, name: "Pod A", status: "Running", courseId: 1 },
    { id: 2, name: "Pod B", status: "Stopped", courseId: 2 },
  ]);

  const [courses] = useState([
    { id: 1, name: "Kubernetes 101" },
    { id: 2, name: "DevOps Fundamentals" },
  ]);

  const [podName, setPodName] = useState("");
  const [selectedCourse, setSelectedCourse] = useState("");

  const addPod = () => {
    if (!podName || !selectedCourse) {
      toast.error("Please fill all fields");
      return;
    }
    const newPod = {
      id: Date.now(),
      name: podName,
      status: "Stopped",
      courseId: parseInt(selectedCourse),
    };
    setPods([...pods, newPod]);
    setPodName("");
    setSelectedCourse("");
    toast.success("Pod added!");
  };

  const deletePod = (id) => {
    setPods(pods.filter((pod) => pod.id !== id));
    toast.success("Pod deleted");
  };

  const toggleStatus = (id) => {
    setPods(
      pods.map((pod) =>
        pod.id === id
          ? {
              ...pod,
              status: pod.status === "Running" ? "Stopped" : "Running",
            }
          : pod
      )
    );
  };

  return (
    <div className="p-6 space-y-6">
      <Card>
        <CardTitle className="mb-2">Add New Pod</CardTitle>
        <CardContent className="flex flex-col md:flex-row gap-4">
          <Input
            placeholder="Pod name"
            value={podName}
            onChange={(e) => setPodName(e.target.value)}
          />
          <select
            value={selectedCourse}
            onChange={(e) => setSelectedCourse(e.target.value)}
            className="border rounded-md px-3 py-2"
          >
            <option value="">Select course</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <Button onClick={addPod}>Add</Button>
        </CardContent>
      </Card>

      <Card>
        <CardTitle className="mb-2">Pods</CardTitle>
        <CardContent>
          {pods.map((pod) => {
            const course = courses.find((c) => c.id === pod.courseId);
            return (
              <div
                key={pod.id}
                className="flex justify-between items-center border-b py-2"
              >
                <div>
                  <span className="font-semibold">{pod.name}</span>{" "}
                  <span className="text-gray-500">
                    (Course: {course?.name || "Unassigned"})
                  </span>
                  <div>
                    {pod.status === "Running" ? "🟢" : "🔴"} {pod.status}
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button onClick={() => toggleStatus(pod.id)}>
                    {pod.status === "Running" ? "Stop" : "Start"}
                  </Button>
                  <Button className="bg-red-500" onClick={() => deletePod(pod.id)}>
                    Delete
                  </Button>
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}
