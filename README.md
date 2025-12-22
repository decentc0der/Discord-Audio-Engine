# Discord Audio Engine & Analytics System

A high-performance Discord music bot built in Java. Unlike standard bots, this project features a persistent **SQLite database** to track listening history and implements a **Vector Space Model** (Cosine Similarity) to mathematically calculate "taste compatibility" between users.

## 🚀 Key Features

* **Fairness Queue Algorithm:** Implements a **PriorityBlockingQueue** to prevent queue hogging.
    * Users are assigned "tickets" based on their recent usage.
    * The scheduler dynamically re-orders the queue to prioritize users with fewer plays, ensuring fair resource allocation even under heavy load.
* **Custom Audio Engine:** Streams high-quality audio from YouTube using Lavaplayer.
* **Persistence Layer:** Uses **SQLite** to store user listening history (schema: `users`, `songs`, `plays`), ensuring data survives restarts.
* **Algorithmic Analysis:** Implements a custom mathematical model to compare user tastes:
    * Constructs frequency vectors based on artist play counts.
    * Calculates the **Cosine Similarity** between two users' vectors to determine a "compatibility percentage."
* **Concurrency:** Database writes are offloaded to background threads to prevent blocking the audio playback thread.
* **Robust Architecture:** Built with the **DAO (Data Access Object)** pattern for clean separation between the Audio Scheduler and the Database.

## 🛠️ Tech Stack

* **Language:** Java 17+
* **Framework:** JDA (Java Discord API)
* **Audio Processing:** Lavaplayer
* **Database:** SQLite (JDBC)
* **Build Tool:** Gradle

## ⚙️ How It Works

### The Compatibility Algorithm
The bot treats a user's listening history as a vector in a multi-dimensional space where each dimension is an Artist.
1.  **Extraction:** SQL `GROUP BY` query aggregates play counts by artist.
2.  **Vectorization:** `User A = { "Drake": 5, "Twice": 2 }`, `User B = { "Drake": 3, "Bach": 1 }`
3.  **Calculation:** The bot computes the cosine of the angle between these vectors:
    $$\text{similarity} = \frac{A \cdot B}{\|A\| \|B\|}$$

## 📦 Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/NathanHung/Discord-Audio-Engine.git](https://github.com/NathanHung/Discord-Audio-Engine.git)
    ```
2.  **Configure Environment:**
    * Create an Environment Variable named `DISCORD_TOKEN`.
    * Paste your Discord Bot Token as the value.
3.  **Build and Run:**
    ```bash
    ./gradlew run
    ```

## 📝 Usage

* `!shout` - Returns the words after shout (but capitalized).
* `!join/!leave` - Bot joins/leaves the channel.
* `!play <search term>` - Adds a song to the queue.
* `!skip` - Skips the current track.
* `!pause/!resume` - Pauses and resumes the current track.
* `!clear` - Clears the entire queue.
* `!compare @User` - Calculates the mathematical similarity between your listening history and another user's.

---
*Created by Nathan Hung as a Systems Engineering project.*