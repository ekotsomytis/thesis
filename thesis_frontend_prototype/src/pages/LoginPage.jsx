import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Button } from "../components/ui/button";

export default function LoginPage() {
  const navigate = useNavigate();

  const handleLogin = () => {
    // (Later add authentication logic here)
    navigate("/dashboard");
  };

  return (
    <div className="flex items-center justify-center h-screen bg-gray-100">
      <Card className="w-[400px]">
        <CardHeader>
          <CardTitle>Professor Login</CardTitle>
        </CardHeader>
        <CardContent>
          <Input placeholder="Email" className="mb-2" />
          <Input placeholder="Password" type="password" />
          <Button className="mt-4 w-full" onClick={handleLogin}>
            Login
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
