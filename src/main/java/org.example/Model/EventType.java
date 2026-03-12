package org.example.Model;

public enum EventType {

    // System-level
    ARRIVAL_SYSTEM,

    // Normal Check-in
    ARRIVAL_NORMAL_CHECKIN,
    NORMAL_CHECKIN_COMPLETE,

    // Self Check-in
    ARRIVAL_SELF_CHECKIN,
    SELF_CHECKIN_COMPLETE,

    // Regular Security
    ARRIVAL_REGULAR_SECURITY,
    REGULAR_SECURITY_COMPLETE,

    // Fast Track Security
    ARRIVAL_FASTTRACK_SECURITY,
    FASTTRACK_SECURITY_COMPLETE,

    // Customs
    ARRIVAL_CUSTOMS,
    CUSTOMS_COMPLETE,

    // Boarding
    ARRIVAL_BOARDING,
    BOARDING_COMPLETE
}
