export const getInitials = (name: string) => {
  const trimmedName = name.trim();
  if (trimmedName === "") {
    return "XX";
  }

  const [firstName, lastName] = trimmedName.split(" ");
  if (!firstName || !lastName) {
    return "XX";
  }

  return `${firstName[0]}${lastName[0]}`;
};
