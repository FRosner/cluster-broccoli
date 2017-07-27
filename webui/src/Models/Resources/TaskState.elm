module Models.Resources.TaskState exposing (..)

import Json.Decode exposing (Decoder, string, fail, succeed, andThen)


{-| The state of a task within an allocation.
-}
type TaskState
    = Dead
    | Running
    | Pending


{-| Decode a task state from JSON.
-}
decoder : Decoder TaskState
decoder =
    string
        |> andThen
            (\name ->
                case name of
                    "dead" ->
                        succeed Dead

                    "running" ->
                        succeed Running

                    "pending" ->
                        succeed Pending

                    _ ->
                        fail ("Unknown task state " ++ name)
            )
