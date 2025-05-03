# WorkoutService

```sql
CREATE TABLE workouts (
    id VARCHAR(255) PRIMARY KEY,
    userid VARCHAR(255) NOT NULL,
    name TEXT NOT NULL,
    date varchar(255) NOT NULL
);

CREATE TABLE exercises (
    id VARCHAR(255) PRIMARY KEY,
    workoutid VARCHAR(255) REFERENCES workouts(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    duration varchar(255) NOT NULL,
    exercise_type VARCHAR(255) NOT NULL,
    sets INTEGER,
    reps INTEGER,
    distance INTEGER,
    steps INTEGER,
    bmp INTEGER,
    speed INTEGER,
    weight INTEGER,
    calories DOUBLE PRECISION,
    reaction VARCHAR(255),
    note TEXT
);

CREATE INDEX idx_exercises_workoutid ON exercises(workoutid);
```