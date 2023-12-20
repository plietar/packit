import { rest } from "msw";
import { authConfigHandlers } from "./handlers/authConfigHandlers";
import { packetGroupHandlers } from "./handlers/packetGroupHandlers";
import { packetGroupSummaryHandlers } from "./handlers/packetGroupSummaryHandlers";
import { packetHandlers } from "./handlers/packetHandler";

// catch any missed requests in tests
const defaultHandlers = [
  rest.get("*", (req, res, ctx) => res(ctx.status(200))),
  rest.post("*", (req, res, ctx) => res(ctx.status(201))),
  rest.patch("*", (req, res, ctx) => res(ctx.status(204))),
  rest.delete("*", (req, res, ctx) => res(ctx.status(204)))
];

export const handlers = [
  ...packetGroupSummaryHandlers,
  ...packetGroupHandlers,
  ...packetHandlers,
  ...authConfigHandlers,
  ...defaultHandlers
];
