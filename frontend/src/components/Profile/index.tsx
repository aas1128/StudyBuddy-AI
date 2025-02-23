"use client";
import Login from "@/components/Login";
import { useAuth0 } from "@auth0/auth0-react";
import Logout from "../Logout";
import { ButtonProps } from "../ui/Button";

const Profile = ({ ...props }: ButtonProps) => {
  const { isAuthenticated, isLoading } = useAuth0();

  if (isLoading) {
    return <div>Loading ...</div>;
  }

  const ButtonComponent = isAuthenticated ? Logout : Login;
  return <ButtonComponent {...props} />;
};

export default Profile;

