import { SquarePlus } from "lucide-react";
import { useState } from "react";
import { Button } from "../../Base/Button";
import { DataTable } from "../common/DataTable";
import { useManageAccessLayoutContext } from "./ManageAccessOutlet";
import { manageRolesColumns } from "./manageRolesColumns";
import { FilterInput } from "../common/FilterInput";
import { PAGE_SIZE } from "../../../../lib/constants";

export const ManageRoles = () => {
  const { roles } = useManageAccessLayoutContext();
  const [filteredName, setFilterByName] = useState("");

  return (
    <>
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Manage Roles</h2>
        <p className="text-muted-foreground">Add roles, update users and permissions on roles</p>
      </div>
      <div className="space-y-4 flex flex-col">
        <div className="flex justify-between">
          <FilterInput setFilter={setFilterByName} placeholder="Filter roles by name..." />
          <Button
            onClick={() => {
              "TODO";
            }}
            variant="outline"
            size="sm"
          >
            <SquarePlus className="mr-2 h-5 w-5" />
            Add Role
          </Button>
        </div>
        <DataTable
          columns={manageRolesColumns}
          data={
            filteredName ? roles.filter((role) => role.name.toLowerCase().includes(filteredName.toLowerCase())) : roles
          }
          pagination={{ pageSize: PAGE_SIZE }}
        />
      </div>
    </>
  );
};
