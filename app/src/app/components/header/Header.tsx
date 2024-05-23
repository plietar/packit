import { PackageOpen } from "lucide-react";
import { NavLink } from "react-router-dom";
import { AccountHeaderDropdown } from "./AccountHeaderDropdown";

import { useUser } from "../providers/UserProvider";
import { ThemeToggleButton } from "./ThemeToggleButton";
import { cn } from "../../../lib/cn";
import { buttonVariants } from "../Base/Button";

export default function Header() {
  const { user } = useUser();

  return (
    <header>
      <div data-testid="header" className="flex-col">
        <div className="border-b shadow-sm dark:shadow-accent">
          <div className="flex h-20 items-center px-4">
            <NavLink to="/">
              <div className="text-xl font-extrabold flex gap-1 items-center">
                <PackageOpen />
                Packit
              </div>
            </NavLink>
            {/* <div className="mx-3 flex items-center md:hidden">
              <NavMenuMobile />
            </div> */}
            {/* {user && <LeftNav className="mx-6 hidden md:flex" />} */}
            <div className="ml-auto flex items-center space-x-4">
              {/* <NavigationLink to="/accessibility" className="mx-6 hidden md:flex">
                Accessibility
              </NavigationLink> */}
              {user?.authorities.includes("user.manage") && (
                <NavLink to="/manage-roles" className={cn(buttonVariants({ variant: "ghost" }), "justify-start")}>
                  Manage Access
                </NavLink>
              )}
              <ThemeToggleButton />
              {user && <AccountHeaderDropdown />}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
