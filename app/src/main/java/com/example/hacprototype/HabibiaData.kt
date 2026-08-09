package com.example.hacprototype

data class Member(
    val memberId: String,
    var firstName: String,
    var lastName: String,
    var email: String,
    val role: String,
    val emailVerified: Boolean,
    val dateJoined: String
) {
    val fullName: String get() = "$firstName $lastName"
}

data class MemberProfile(
    val profileId: String,
    val memberId: String,
    var experienceLevel: String,
    var bowType: String,
    var division: String,
    var emergencyContact: String
)

data class ClubEvent(
    val eventId: String,
    var title: String,
    var description: String,
    var eventDate: String,
    var eventTime: String,
    var location: String,
    var type: String = "Event"
)

data class Competition(
    val competitionId: String,
    var competitionName: String,
    var competitionDate: String,
    var registrationDeadline: String,
    var venue: String,
    var description: String,
    var status: String = "UPCOMING"
)

data class Score(
    val scoreId: String,
    val memberId: String,
    var scoreValue: Int,
    var scoreDate: String,
    var event: String,
    var notes: String
)

data class Announcement(
    val announcementId: String,
    var title: String,
    var content: String,
    var datePosted: String
)

data class Notification(
    val notificationId: String,
    var title: String,
    var message: String,
    var dateSent: String,
    var isRead: Boolean
)

data class BeginnerResource(
    val resourceId: String,
    var title: String,
    var category: String,
    var description: String,
    var resourceLink: String
)

object HabibiaSession {
    var isAdmin: Boolean = false
    var selectedMemberTab: Int = R.id.navHome
    var selectedEventId: String? = null
    var selectedCompetitionId: String? = null
    var selectedScoreId: String? = null
    var selectedResourceId: String? = null
    var selectedAnnouncementId: String? = null
    var editingEventId: String? = null
    var editingCompetitionId: String? = null
    var editingAnnouncementId: String? = null
    var editingResourceId: String? = null
    var pendingSnackbar: String? = null
    var progressRange: String = "MONTH"
}

object HabibiaDummyData {
    var member = Member(
        memberId = "M001",
        firstName = "Aaliyah",
        lastName = "Smit",
        email = "member@habibia.co.za",
        role = "Member",
        emailVerified = true,
        dateJoined = "2024-01-10"
    )

    var admin = Member(
        memberId = "A001",
        firstName = "Admin",
        lastName = "Habibia",
        email = "admin@habibia.co.za",
        role = "Admin",
        emailVerified = true,
        dateJoined = "2023-01-10"
    )

    var profile = MemberProfile(
        profileId = "P001",
        memberId = "M001",
        experienceLevel = "Intermediate",
        bowType = "Recurve",
        division = "Recurve Women",
        emergencyContact = "+27 82 555 0199"
    )

    val members = mutableListOf(
        member,
        Member("M002", "Jordan", "Naidoo", "jordan@habibia.co.za", "Member", true, "2024-03-12"),
        Member("M003", "Sam", "Davids", "sam@habibia.co.za", "Member", true, "2024-05-02"),
        admin
    )

    val events = mutableListOf(
        ClubEvent("E001", "Saturday Practice", "Weekly training and scoring session.", "2026-09-12", "09:00", "Habibia Archery Club", "Practice"),
        ClubEvent("E002", "Club Indoor League", "Indoor league night for members.", "2026-09-18", "18:30", "Habibia Archery Club", "Event"),
        ClubEvent("E003", "Junior Development Day", "Introductory coaching session.", "2026-09-25", "10:00", "Habibia Archery Club", "Event")
    )

    val competitions = mutableListOf(
        Competition("C001", "Western Cape Indoor Championships", "2026-09-15", "2026-09-05", "Cape Town", "Indoor competition for club members.", "REGISTRATION OPEN"),
        Competition("C002", "Habibia Open Tournament", "2026-10-03", "2026-09-20", "Habibia Archery Club", "Club-level competition and scoring day.", "UPCOMING"),
        Competition("C003", "Regional Outdoor Shoot", "2026-10-24", "2026-10-08", "Cape Town Range", "Outdoor target scoring competition.", "UPCOMING")
    )

    val scores = mutableListOf(
        Score("S001", "M001", 72, "2026-09-10", "Saturday Practice", "Strong morning practice, consistent 10s."),
        Score("S002", "M001", 78, "2026-09-03", "Club Training", "Good shot sequence and scoring."),
        Score("S003", "M001", 81, "2026-08-28", "Monthly Shoot", "Best score this month."),
        Score("S004", "M001", 76, "2026-08-17", "Club Training", "Improved focus after warm-up.")
    )

    val announcements = mutableListOf(
        Announcement("A001", "New Indoor League", "The indoor league begins this Saturday.", "2026-09-02"),
        Announcement("A002", "Equipment Safety Check", "Please inspect bowstrings before training.", "2026-09-04"),
        Announcement("A003", "Club Social Shoot", "Members are invited to participate in the social shoot.", "2026-09-06")
    )

    val notifications = mutableListOf(
        Notification("N001", "New Competition Added", "Western Cape Indoor Championships is open for registration.", "2026-09-01 08:00", false),
        Notification("N002", "Practice Reminder", "Saturday practice time has changed.", "2026-09-05 17:15", false),
        Notification("N003", "Score Recorded", "Your latest score has been recorded.", "2026-09-03 10:24", true)
    )

    val resources = mutableListOf(
        BeginnerResource("R001", "Archery Safety Basics", "Safety", "Learn the essential safety rules every beginner should know.", "https://example.com/safety"),
        BeginnerResource("R002", "How to Hold a Recurve Bow", "Technique", "Learn about grip, posture, and stance for a recurve bow.", "https://example.com/grip"),
        BeginnerResource("R003", "Understanding Archery Equipment", "Equipment", "Get familiar with bows, arrows, strings, and accessories.", "https://example.com/equipment"),
        BeginnerResource("R004", "Basic Shooting Technique", "Technique", "Improve alignment, release, and follow-through.", "https://example.com/technique"),
        BeginnerResource("R005", "Scoring Explained", "Scoring", "Learn target scoring and round structure.", "https://example.com/scoring"),
        BeginnerResource("R006", "Your First Practice Day", "Getting Started", "A simple checklist for your first day at Habibia Archery Club.", "https://example.com/first-day")
    )

    fun nextEvent(): ClubEvent? = events.minByOrNull { it.eventDate }
    fun nextCompetition(): Competition? = competitions.minByOrNull { it.competitionDate }

    fun memberScores(): List<Score> = scores.filter { it.memberId == member.memberId }
        .sortedByDescending { it.scoreDate }

    fun latestScore(): Score? = memberScores().firstOrNull()
    fun averageScore(): Int {
        val list = memberScores()
        if (list.isEmpty()) return 0
        return list.map { it.scoreValue }.average().toInt()
    }

    fun highestScore(): Int = memberScores().maxOfOrNull { it.scoreValue } ?: 0
    fun lowestScore(): Int = memberScores().minOfOrNull { it.scoreValue } ?: 0

    fun unreadCount(): Int = notifications.count { !it.isRead }

    fun greeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
