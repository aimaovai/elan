import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ReservationFlow } from "./ReservationFlow";

describe("ReservationFlow", () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          date: "2026-10-30",
          partySize: 2,
          timezone: "America/Los_Angeles",
          availableSlots: [{ time: "17:30", available: true }],
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: async () => ({
          confirmationCode: "ELN-ABCD2345",
          status: "CONFIRMED",
          reservationDate: "2026-10-30",
          reservationTime: "17:30",
          endTime: "19:30",
          partySize: 2,
          tableName: "Table 01",
          experienceName: null,
          managementToken: "private-management-token-value-that-is-long-enough",
        }),
      }));
  });

  it("submits a real booking only after selecting an available time", async () => {
    render(<ReservationFlow />);
    fireEvent.click(await screen.findByRole("button", { name: "17:30" }));
    fireEvent.change(screen.getByLabelText("First name"), { target: { value: "Jordan" } });
    fireEvent.change(screen.getByLabelText("Last name"), { target: { value: "Morgan" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "jordan@example.test" } });
    fireEvent.change(screen.getByLabelText("Phone"), { target: { value: "+1 555 010 0100" } });
    const selectedDate = (screen.getByLabelText("Date") as HTMLInputElement).value;
    fireEvent.click(screen.getByRole("button", { name: "Confirm reservation" }));

    expect(await screen.findByText(/ELN-ABCD2345/)).toBeInTheDocument();
    expect(sessionStorage.getItem("elan-reservation-management-token")).toContain("private-management-token");
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
    const bookingCall = vi.mocked(fetch).mock.calls[1];
    expect(bookingCall[0]).toContain("/reservations");
    expect(bookingCall[1]?.headers).toMatchObject({ "Idempotency-Key": expect.any(String) });
    expect(JSON.parse(String(bookingCall[1]?.body))).toMatchObject({
      firstName: "Jordan",
      reservationDate: selectedDate,
      reservationTime: "17:30",
      partySize: 2,
    });
  });
});
