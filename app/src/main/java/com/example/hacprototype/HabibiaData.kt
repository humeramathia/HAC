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

enum class SessionType { PRACTICE, LEAGUE }

data class ArrowScore(
    val value: Int,
    val isX: Boolean = false
) {
    val label: String get() = if (isX) "X" else value.toString()
}

data class ScoreEnd(
    val endNumber: Int,
    val arrows: MutableList<ArrowScore> = mutableListOf()
) {
    val total: Int get() = arrows.sumOf { it.value }
    fun arrowLabels(): String = arrows.joinToString("  |  ") { it.label }
}

data class ScoreSession(
    val sessionId: String,
    val memberId: String,
    val type: SessionType,
    var title: String,
    var distanceMeters: Int,
    var arrowsPerEnd: Int,
    var numberOfEnds: Int,
    var date: String,
    var notes: String = "",
    var ends: MutableList<ScoreEnd> = mutableListOf(),
    var ranking: Int? = null,
    var fieldSize: Int? = null,
    var leagueName: String? = null
) {
    val totalArrows: Int get() = arrowsPerEnd * numberOfEnds
    val maxScore: Int get() = totalArrows * 10
    val totalScore: Int get() = ends.sumOf { it.total }
    val arrowsShot: Int get() = ends.sumOf { it.arrows.size }
    val completedEnds: Int get() = ends.size
    val averageArrow: Double
        get() = if (arrowsShot == 0) 0.0 else totalScore.toDouble() / arrowsShot
    val averageEnd: Double
        get() = if (ends.isEmpty()) 0.0 else ends.map { it.total }.average()
    val tensCount: Int
        get() = ends.sumOf { end -> end.arrows.count { it.value == 10 } }
    val xCount: Int
        get() = ends.sumOf { end -> end.arrows.count { it.isX } }
    val highestEnd: Int get() = ends.maxOfOrNull { it.total } ?: 0
    val lowestEnd: Int get() = ends.minOfOrNull { it.total } ?: 0
    val typeLabel: String get() = if (type == SessionType.PRACTICE) "Practice" else "League"
    val distanceLabel: String get() = "$distanceMeters m"
    val scoreLabel: String get() = "$totalScore / $maxScore"

    fun toScore(): Score = Score(
        scoreId = sessionId,
        memberId = memberId,
        scoreValue = totalScore,
        scoreDate = date,
        event = title,
        notes = notes
    )
}

object LeagueStandard {
    const val TOTAL_ARROWS = 60
    const val ARROWS_PER_END = 6
    const val DEFAULT_DISTANCE = 18
    val numberOfEnds: Int get() = TOTAL_ARROWS / ARROWS_PER_END
}

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
    var selectedSessionId: String? = null
    var selectedResourceId: String? = null
    var selectedAnnouncementId: String? = null
    var editingEventId: String? = null
    var editingCompetitionId: String? = null
    var editingAnnouncementId: String? = null
    var editingResourceId: String? = null
    var pendingSnackbar: String? = null
    var progressRange: String = "MONTH"
    var progressType: SessionType = SessionType.PRACTICE
    var draftSession: ScoreSession? = null
    var currentEndArrows: MutableList<ArrowScore> = mutableListOf()
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

    val scoreSessions = mutableListOf(
        buildSession(
            id = "PS001",
            type = SessionType.PRACTICE,
            title = "Saturday Practice",
            distance = 18,
            date = "2026-09-12",
            notes = "Strong grouping after the warm-up ends.",
            arrowsPerEnd = 6,
            ends = listOf(
                listOf("9", "8", "10", "9", "7", "9"),
                listOf("10", "9", "8", "9", "X", "8"),
                listOf("8", "7", "9", "8", "10", "8"),
                listOf("10", "9", "9", "8", "8", "9"),
                listOf("8", "9", "7", "10", "8", "9"),
                listOf("10", "X", "9", "9", "8", "9"),
                listOf("7", "8", "10", "8", "8", "8"),
                listOf("9", "8", "10", "8", "9", "8"),
                listOf("X", "9", "9", "9", "8", "9"),
                listOf("9", "10", "8", "9", "9", "9")
            )
        ),
        buildSession(
            id = "PS002",
            type = SessionType.PRACTICE,
            title = "Evening Practice",
            distance = 20,
            date = "2026-09-10",
            notes = "Worked on clicker timing at 20 m.",
            arrowsPerEnd = 6,
            ends = listOf(
                listOf("8", "8", "9", "7", "8", "8"),
                listOf("9", "8", "8", "9", "7", "8"),
                listOf("8", "9", "10", "8", "8", "7"),
                listOf("9", "8", "7", "8", "9", "8"),
                listOf("8", "7", "9", "8", "8", "8"),
                listOf("9", "9", "8", "8", "7", "8"),
                listOf("8", "8", "9", "7", "8", "9"),
                listOf("7", "8", "8", "9", "8", "8"),
                listOf("9", "8", "8", "8", "7", "9"),
                listOf("8", "9", "8", "8", "8", "8")
            )
        ),
        buildSession(
            id = "PS003",
            type = SessionType.PRACTICE,
            title = "Club Training",
            distance = 18,
            date = "2026-09-03",
            notes = "Good shot sequence through the middle ends.",
            arrowsPerEnd = 6,
            ends = listOf(
                listOf("8", "9", "9", "8", "8", "9"),
                listOf("9", "10", "8", "8", "9", "8"),
                listOf("8", "8", "9", "9", "8", "8"),
                listOf("10", "9", "8", "9", "8", "8"),
                listOf("9", "8", "8", "9", "9", "8"),
                listOf("8", "9", "10", "8", "8", "9"),
                listOf("9", "8", "8", "8", "9", "8"),
                listOf("8", "9", "9", "8", "8", "8"),
                listOf("9", "8", "10", "8", "8", "9"),
                listOf("8", "9", "8", "9", "8", "9")
            )
        ),
        buildSession(
            id = "PS004",
            type = SessionType.PRACTICE,
            title = "Outdoor Practice",
            distance = 30,
            date = "2026-08-22",
            notes = "Windy first half, settled later.",
            arrowsPerEnd = 6,
            ends = listOf(
                listOf("7", "8", "8", "7", "8", "8"),
                listOf("8", "8", "9", "7", "8", "8"),
                listOf("8", "9", "8", "8", "7", "8"),
                listOf("9", "8", "8", "8", "8", "8"),
                listOf("8", "8", "9", "8", "8", "7"),
                listOf("8", "9", "8", "8", "8", "8"),
                listOf("9", "8", "8", "9", "8", "8"),
                listOf("8", "8", "9", "8", "8", "8")
            )
        ),
        buildSession(
            id = "LS001",
            type = SessionType.LEAGUE,
            title = "September League",
            distance = 18,
            date = "2026-09-18",
            notes = "Solid qualification-style indoor round.",
            arrowsPerEnd = LeagueStandard.ARROWS_PER_END,
            ends = listOf(
                listOf("10", "X", "9", "9", "9", "9"),
                listOf("10", "10", "9", "9", "8", "9"),
                listOf("9", "10", "9", "9", "8", "9"),
                listOf("10", "9", "8", "9", "9", "8"),
                listOf("10", "X", "9", "9", "9", "8"),
                listOf("9", "10", "9", "9", "8", "9"),
                listOf("8", "9", "10", "8", "9", "8"),
                listOf("10", "X", "10", "9", "8", "9"),
                listOf("9", "9", "10", "8", "8", "9"),
                listOf("10", "9", "9", "9", "8", "9")
            ),
            ranking = 2,
            fieldSize = 12,
            leagueName = "September League"
        ),
        buildSession(
            id = "LS002",
            type = SessionType.LEAGUE,
            title = "August League",
            distance = 18,
            date = "2026-08-21",
            notes = "Held form through the last four ends.",
            arrowsPerEnd = LeagueStandard.ARROWS_PER_END,
            ends = listOf(
                listOf("9", "10", "9", "8", "9", "8"),
                listOf("10", "9", "9", "8", "9", "8"),
                listOf("9", "9", "8", "10", "8", "9"),
                listOf("8", "9", "10", "8", "9", "8"),
                listOf("10", "X", "8", "9", "8", "9"),
                listOf("9", "8", "9", "9", "8", "9"),
                listOf("8", "10", "9", "8", "8", "9"),
                listOf("9", "9", "9", "8", "10", "8"),
                listOf("10", "8", "9", "9", "8", "8"),
                listOf("9", "9", "8", "10", "8", "9")
            ),
            ranking = 3,
            fieldSize = 12,
            leagueName = "August League"
        ),
        buildSession(
            id = "LS003",
            type = SessionType.LEAGUE,
            title = "July League",
            distance = 18,
            date = "2026-07-17",
            notes = "First indoor league of the season.",
            arrowsPerEnd = LeagueStandard.ARROWS_PER_END,
            ends = listOf(
                listOf("8", "9", "9", "8", "9", "8"),
                listOf("9", "8", "10", "8", "8", "9"),
                listOf("8", "9", "8", "9", "8", "9"),
                listOf("9", "9", "8", "8", "9", "8"),
                listOf("10", "8", "8", "9", "8", "8"),
                listOf("8", "9", "9", "8", "9", "8"),
                listOf("9", "8", "8", "10", "8", "8"),
                listOf("8", "9", "9", "8", "8", "9"),
                listOf("9", "8", "10", "8", "8", "8"),
                listOf("8", "9", "8", "9", "9", "8")
            ),
            ranking = 4,
            fieldSize = 11,
            leagueName = "July League"
        )
    )

    val scores = mutableListOf<Score>().apply {
        addAll(scoreSessions.map { it.toScore() })
    }

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

    fun memberSessions(): List<ScoreSession> = scoreSessions
        .filter { it.memberId == member.memberId }
        .sortedByDescending { it.date }

    fun practiceSessions(): List<ScoreSession> =
        memberSessions().filter { it.type == SessionType.PRACTICE }

    fun leagueSessions(): List<ScoreSession> =
        memberSessions().filter { it.type == SessionType.LEAGUE }

    fun latestScore(): Score? = memberSessions().maxByOrNull { it.date }?.toScore()
    fun averageScore(): Int {
        val list = memberSessions()
        if (list.isEmpty()) return 0
        return list.map { it.totalScore }.average().toInt()
    }

    fun highestScore(): Int = memberSessions().maxOfOrNull { it.totalScore } ?: 0
    fun lowestScore(): Int = memberSessions().minOfOrNull { it.totalScore } ?: 0

    fun findSession(id: String?): ScoreSession? =
        scoreSessions.find { it.sessionId == id } ?: memberSessions().firstOrNull()

    fun addCompletedSession(session: ScoreSession) {
        scoreSessions.add(0, session)
        scores.add(0, session.toScore())
    }

    fun sessionImprovement(type: SessionType): Double {
        val ordered = memberSessions().filter { it.type == type }.sortedBy { it.date }
        if (ordered.size < 2) return 0.0
        val first = ordered.first().totalScore.toDouble()
        val latest = ordered.last().totalScore.toDouble()
        if (first == 0.0) return 0.0
        return ((latest - first) / first) * 100.0
    }

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

private fun parseArrow(token: String): ArrowScore {
    return if (token.equals("X", ignoreCase = true)) ArrowScore(10, true)
    else ArrowScore(token.toInt(), false)
}

private fun buildSession(
    id: String,
    type: SessionType,
    title: String,
    distance: Int,
    date: String,
    notes: String,
    arrowsPerEnd: Int,
    ends: List<List<String>>,
    ranking: Int? = null,
    fieldSize: Int? = null,
    leagueName: String? = null
): ScoreSession {
    val scoredEnds = ends.mapIndexed { index, tokens ->
        ScoreEnd(index + 1, tokens.map(::parseArrow).toMutableList())
    }.toMutableList()
    return ScoreSession(
        sessionId = id,
        memberId = "M001",
        type = type,
        title = title,
        distanceMeters = distance,
        arrowsPerEnd = arrowsPerEnd,
        numberOfEnds = ends.size,
        date = date,
        notes = notes,
        ends = scoredEnds,
        ranking = ranking,
        fieldSize = fieldSize,
        leagueName = leagueName
    )
}
