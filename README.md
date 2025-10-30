# Clan Rank Up Notifier

A plugin that checks your clan members’ join dates and notifies you when they are eligible for a rank-up based on configurable time-in-clan rules.

---

## Features

- **Automatic rank checks** - detects members who have been in the clan long enough for the next rank.
- **Custom rules** - define your own day thresholds and corresponding ranks.
- **Manual refresh** - click the **Update** button in the sidebar to run a check at any time.
- **Filtered results** - limit checks to specific current ranks and ignore certain members.
- **Desktop notifications** - optional pop-ups for newly eligible members.
- **Compact table UI** - shows name, days in clan, current rank, and next eligible rank.
- **Ignore lists** - ignores manually configured user list
---

## Configuration

Open the plugin settings in RuneLite and adjust:

| Setting | Description |
|----------|--------------|
| **Eligible current ranks** | Only members whose current rank matches one of these (comma-separated) will be checked. Leave empty to include all. |
| **Rank rules** | Define thresholds in days and target ranks. Example:<br>`7=Recruit`<br>`30=Corporal`<br>`60=Sergeant` |
| **Ignored users** | Comma-separated list of usernames to skip. |
| **Notify on login** | Sends a quick test notification when logging in to confirm that the plugin works. |

---

## How it works

1. The plugin reads join dates for all clan members.
2. It compares each member’s days-in-clan to the configured rule table.
3. If their current rank doesn’t match the required rank for their time, and they’re not ignored, they appear in the sidebar list.
4. You’ll get a RuneLite notification the first time each member becomes eligible each day.

---

## Panel Overview

| Column | Meaning |
|---------|----------|
| **Name** | Member’s display name (wrapped if long). |
| **Days** | Days since joining the clan. |
| **Curr** | Current rank title. |
| **Next** | Target rank based on your rule set. |

---

## Notes

- Compatible with standard clans
- Join dates are read via the public API available to the client; no external data is used.
- All comparisons are case-insensitive and trimmed.

---

## Example Rules
Days = rank

```ini
7=Recruit
30=Corporal
60=Sergeant
120=Lieutenant
```

This would notify you when a member passes 30 days and is still a Recruit, or passes 60 days and is still a Corporal, etc.


## Developer

Written for RuneLite by **serverlat**.  
Source code is licensed under the BSD 2-Clause License.
