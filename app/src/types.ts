import {useDispatch} from "react-redux";
import store, {rootReducer} from "./app/store/store";

export enum SideBarItems {
    explorer,
    packetRunner,
    workflowRunner,
    projectDoc
}

export interface RejectedErrorValue {
    rejectValue: Error
}

export interface Packet {
    id: string,
    name: string,
    displayName: string,
    published: boolean,
    parameters: Record<string, string>
}

export interface Header {
    label: string,
    accessor: keyof Packet,
    sortable: boolean
}

export interface PacketTableProps {
    data: Packet[]
}

export interface PacketsState {
    packets: Packet[]
    fetchPacketsError:  null | Error
    packet: PacketMetadata,
    packetError: null | Error,
    fileUrl: string,
    fileUrlError: null | Error
}

export interface PacketMetadata {
    id: string
    name: string
    displayName?: string
    published?: boolean
    parameters: Record<string, string> | null
    time?: Record<string, string>
    files: File[]
    custom?: Custom
}

export interface Custom {
    orderly: {
        artefacts: Artefact[]
        description: Description
    }
}

interface Description {
    custom: Record<string, string>
    display: string
}

interface Artefact {
    description: string
    paths: string[]
}

interface File {
    path: string,
    size: number,
    hash: string
}

export interface Error {
    error: {
        detail: string
        error: string
    }
}

export type RootState = ReturnType<typeof rootReducer>;

export type AppDispatch = typeof store.dispatch;

export const useAppDispatch = () => useDispatch<AppDispatch>();
