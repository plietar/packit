import useSWR from "swr";
import { RoleWithRelationships } from "../types/RoleWithRelationships";
import appConfig from "../../../../../config/appConfig";
import { fetcher } from "../../../../../lib/fetch";
import { getUsersWithRoles } from "../utils/getUsersWithRoles";

export const useGetRolesWithRelationships = () => {
  const { data, isLoading, error } = useSWR<RoleWithRelationships[]>(`${appConfig.apiUrl()}/role`, (url: string) =>
    fetcher({ url })
  );
  const nonUsernameRoles = data ? data.filter((role) => !role.isUsername) : [];
  const usernameRoles = data ? data.filter((role) => role.isUsername) : [];
  const users = data ? getUsersWithRoles(nonUsernameRoles, usernameRoles) : [];

  return {
    roles: nonUsernameRoles,
    users,
    isLoading,
    error
  };
};
