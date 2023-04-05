import React, {useState} from "react";
import {AssessorsProperty, Header, PacketTableProps} from "../../../../types";
import {FaSort} from "react-icons/fa";

const headers: Header[] = [
    {label: "Name", accessor: "displayName", sortable: true},
    {label: "Version", accessor: "id", sortable: false},
    {label: "Status", accessor: "published", sortable: true},
    {label: "Parameters", accessor: "parameters", sortable: false}
];

export default function Table({data}: PacketTableProps) {
    const [ascending, setAscending] = useState(true);

    const sortPackets = (accessor: string) => {
        data.sort((a, b) => {
            setAscending(!ascending);
            return (`${a[accessor as keyof AssessorsProperty]}`
                .localeCompare(`${b[accessor as keyof AssessorsProperty]}`)) * (ascending ? -1 : 1);
        });
    };

    return (
        <table data-testid="table" className="table table-hover table-bordered table-sm">
            <thead>
            <tr>
                {headers.map(({label, accessor, sortable}) => (
                    <th key={accessor}>
                        <span className="m-4"
                              onClick={() => sortable ? sortPackets(accessor) : null}>{label}
                        </span>
                        {
                            sortable ? <span className="icon-sort"><FaSort/></span> : ""
                        }
                    </th>
                ))}
            </tr>
            </thead>
            <tbody>
            {data.map((packet, key) => (
                <tr key={`row-${key}`}>
                    <td>
                        <span>
                            <a href="#">{packet.displayName ? packet.displayName : packet.name}</a>
                        </span>
                    </td>
                    <td>
                        <span>{packet.id}</span>
                    </td>
                    <td>
                        <span className={`badge ${packet.published ? "badge-published" : "badge-internal"}`}>
                                {packet.published ? "published" : "internal"}
                        </span>
                    </td>
                    <td>
                        <span>
                            <ul className="list-unstyled ">
                                {Object.entries(packet.parameters).map(([key, value]) => (
                                    <li className="justify-content-evenly" key={`col-${key}`}>{key}={value}</li>
                                ))}
                            </ul>
                        </span>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
}
