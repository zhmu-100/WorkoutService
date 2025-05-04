# WorkoutService

## About

Default configuration for this microservice in env:

```
PORT=8001

DB_MODE=LOCAL        # LOCAL or gateway
DB_HOST=localhost
DB_PORT=8081

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

Default port for this service is 8002\. [App.kt](app/src/main/kotlin/com/training/workout/App.kt)/

### Routes:

Workout routes:

- GET workout/workouts/{id} - Get a workout by ID
- GET workout/workouts?user_id={userId}&page={page No}&page_size={page size} - List all workouts for a specific user
- POST workout/workouts - Create a new workout
- PUT workout/workouts/{id} - Update a workout
- DELETE workout/workouts/{id}?user_id={userId} - Delete a workout
- GET workout/workouts/{id}/exercises - Get all exercises for a specific workout
- POST "something" - Create custom exercise

### Workouts query examples

**Create workout**

URL:

```
POST http://localhost:8001/training/workouts
```

Body:

```json
{
  "userId": "u1",
  "name": "Morning push‑ups",
  "excercises":[
    {
      "name":"EXERCISE_NAME_PUSHUPS",
      "duration":"2025-04-12T14:20:06.417571800",
      "excercise_type":"EXERCISE_TYPE_DYNAMIC",
      "sets":4,
      "reps":20
    }
  ]
}
```

Response:

```json
{
    "id": "216d8a66-f92e-46ff-9d79-32568b475b78",
    "userId": "u1",
    "name": "Morning push‑ups",
    "date": "2025-05-03T12:09:46.706784300",
    "excercises": [
        {
            "id": "e8463334-b155-4a48-b6b5-bf2e1cfc74bd",
            "name": "EXERCISE_NAME_PUSHUPS",
            "duration": "2025-04-12T14:20:06.417571800",
            "excercise_type": "EXERCISE_TYPE_DYNAMIC",
            "sets": 4,
            "reps": 20
        }
    ]
}
```

**Create workout with 2 exercises**

URL:

```
POST http://localhost:8001/training/workouts
```

Body:

```json
{
  "userId": "u2",
  "name": "Cycling session",
  "excercises": [
    {
      "name": "EXERCISE_NAME_CYCLING",
      "duration": "2025-05-05T07:00:00",
      "excercise_type": "EXERCISE_TYPE_DYNAMIC",
      "distance": 20000,
      "speed": 25,
      "calories": 730
    },
    {
      "name": "EXERCISE_NAME_PLANK",
      "duration": "2025-05-05T07:45:00",
      "excercise_type": "EXERCISE_TYPE_STATIC",
      "duration": "2025-05-05T00:03:00",
      "reaction": "EXERCISE_REACTION_GOOD"
    }
  ]
}
```

Response:

```json
{
    "id": "31461cf7-70ec-4ca5-b6e6-ad9094edc6ec",
    "userId": "u2",
    "name": "Cycling session",
    "date": "2025-05-03T12:10:56.046376200",
    "excercises": [
        {
            "id": "44b6fa24-4a92-4d9c-b607-87beca47e868",
            "name": "EXERCISE_NAME_CYCLING",
            "duration": "2025-05-05T07:00",
            "excercise_type": "EXERCISE_TYPE_DYNAMIC",
            "distance": 20000,
            "speed": 25,
            "calories": 730.0
        },
        {
            "id": "cc2871df-248c-42cc-a8a6-8ac82655dd5e",
            "name": "EXERCISE_NAME_PLANK",
            "duration": "2025-05-05T00:03",
            "excercise_type": "EXERCISE_TYPE_STATIC",
            "reaction": "EXERCISE_REACTION_GOOD"
        }
    ]
}
```

**Get workout by ID**

URL (body is empty):

```
GET http://localhost:8001/training/workouts/216d8a66-f92e-46ff-9d79-32568b475b78
```

Response:

```json
{
    "id": "216d8a66-f92e-46ff-9d79-32568b475b78",
    "userId": "u1",
    "name": "Morning push‑ups (updated name)",
    "date": "2025-05-03T12:17:12.593849700",
    "excercises": [
        {
            "id": "e8463334-b155-4a48-b6b5-bf2e1cfc74bd",
            "name": "EXERCISE_NAME_PUSHUPS",
            "duration": "2025-04-12T14:20:06.417571800",
            "excercise_type": "EXERCISE_TYPE_DYNAMIC",
            "sets": 4,
            "reps": 20
        }
    ]
}
```

**_List all workouts_**

URL (body is empty):

```
GET http://localhost:8001/training/workouts
```

Response:

```json
[
    {
        "id": "31461cf7-70ec-4ca5-b6e6-ad9094edc6ec",
        "userId": "u2",
        "name": "Cycling session",
        "date": "2025-05-03T12:10:56.046376200"
    },
    {
        "id": "9488297c-ddf4-4d51-9644-5bc3bc88eb9f",
        "userId": "u1",
        "name": "Evening run",
        "date": "2025-05-03T12:10:01.456972100"
    },
    {
        "id": "216d8a66-f92e-46ff-9d79-32568b475b78",
        "userId": "u1",
        "name": "Morning push‑ups",
        "date": "2025-05-03T12:09:46.706784300"
    }
]
```

**List all workouts for specific user with pagination**

URL (body is empty):

```
GET http://localhost:8001/training/workouts?user_id=u1&page=1&page_size=3
```

**Update workout**

Notice, that only name is updated. Exercises are not updated, but they are still in the database.

URL:

```
PUT http://localhost:8001/training/workouts/216d8a66-f92e-46ff-9d79-32568b475b78
```

Body:

```json
{
  "userId": "u1",
  "name": "Morning push‑ups (updated name)",
  "excercises": []
}
```

Response:

```json
{
    "id": "216d8a66-f92e-46ff-9d79-32568b475b78",
    "userId": "u1",
    "name": "Morning push‑ups (updated name)",
    "date": "2025-05-03T12:17:12.593849700"
}
```

**Delete workout**

URL (body is empty):

```
DELETE http://localhost:8001/training/workouts/9488297c-ddf4-4d51-9644-5bc3bc88eb9f?user_id=u1
```

Response:

```
Workout deleted
```

***Get all exercises for a specific workout**

URL (body is empty):

```
GET http://localhost:8001/training/workouts/31461cf7-70ec-4ca5-b6e6-ad9094edc6ec/exercises
```

Response:

```json
[
    {
        "id": "44b6fa24-4a92-4d9c-b607-87beca47e868",
        "name": "EXERCISE_NAME_CYCLING",
        "duration": "2025-05-05T07:00",
        "excercise_type": "EXERCISE_TYPE_DYNAMIC",
        "distance": 20000,
        "speed": 25,
        "calories": 730.0
    },
    {
        "id": "cc2871df-248c-42cc-a8a6-8ac82655dd5e",
        "name": "EXERCISE_NAME_PLANK",
        "duration": "2025-05-05T00:03",
        "excercise_type": "EXERCISE_TYPE_STATIC",
        "reaction": "EXERCISE_REACTION_GOOD"
    }
]
```

## Notes

Workouts and exercises data formats:

```proto
enum ExcerciseReaction {
  EXCERCISE_REACTION_UNSPECIFIED = 0;
  EXCERCISE_REACTION_EXCELLENT = 1;
  EXCERCISE_REACTION_GOOD = 2;
  EXCERCISE_REACTION_OK = 3;
  EXCERCISE_REACTION_BAD = 4;
  EXCERCISE_REACTION_VERY_BAD = 5;
}

enum ExcerciseType {
  EXCERCISE_TYPE_UNSPECIFIED = 0;
  EXCERCISE_TYPE_STATIC = 1;
  EXCERCISE_TYPE_DYNAMIC = 2;
}

enum ExcerciseName {
  EXCERCISE_NAME_UNSPECIFIED = 0;
  EXCERCISE_NAME_PUSHUPS = 1;
  EXCERCISE_NAME_PULLUPS = 2;
  EXCERCISE_NAME_SQUATS = 3;
  EXCERCISE_NAME_PLANK = 4;
  EXCERCISE_NAME_RUNNING = 5;
  EXCERCISE_NAME_CYCLING = 6;
}

message Excercise {
  string id = 1;
  ExcerciseName name = 2;
  google.protobuf.Timestamp duration = 3;
  ExcerciseType excercise_type = 4;
  optional int32 sets = 5;
  optional int32 reps = 6;
  optional int32 distance = 7;
  optional int32 steps = 8;
  optional int32 bmp = 9;
  optional int32 speed = 10;
  optional int32 weight = 11;
  optional double calories = 12;
  optional ExcerciseReaction reaction = 13;
  optional string note = 14;
}

message Workout {
  string id = 1;
  string name = 2;
  google.protobuf.Timestamp date = 3;
  repeated Excercise excercises = 4;
}
```

Actions for workouts:

```proto
service TrainingService {
  rpc CreateWorkout(Workout) returns (Workout);
  rpc GetWorkout(GetWorkoutRequest) returns (Workout);
  rpc ListWorkouts(google.protobuf.Empty) returns (ListWorkoutsResponse);
  rpc UpdateWorkout(Workout) returns (Workout);
  rpc DeleteWorkout(DeleteWorkoutRequest) returns (google.protobuf.Empty);

  rpc GetWorkoutExcercises(GetWorkoutRequest) returns (GetWorkoutExcercisesResponse);
  rpc CreateCustomWorkout(Workout) returns (CreateCustomWorkoutResponse);
}

message GetWorkoutRequest {
  string id = 1;
}

message ListWorkoutsResponse {
  repeated Workout workouts = 1;
}

message DeleteWorkoutRequest {
  string id = 1;
}

message GetWorkoutExcercisesResponse {
  repeated Excercise excercises = 1;
}

message CreateCustomWorkoutResponse {
  string id = 1;
}
```

## SQL

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
