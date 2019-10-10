module Models.Resources.TaskState exposing (..)

import Json.Decode exposing (Decoder, andThen, fail, string, succeed)


{-| The state of a task within an allocation.
-}
type TaskState
    = TaskDead
    | TaskRunning
    | TaskPending


{-| Decode a task state from JSON.
-}
decoder : Decoder TaskState
decoder =
    string
        |> andThen
            (\name ->
                case name of
                    "dead" ->
                        succeed TaskDead

                    "running" ->
                        succeed TaskRunning

                    "pending" ->
                        succeed TaskPending

                    _ ->
                        fail ("Unknown task state " ++ name)
            )
