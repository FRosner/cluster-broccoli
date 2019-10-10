module Models.Resources.ClientStatus exposing (..)

import Json.Decode exposing (Decoder, andThen, fail, string, succeed)


{-| The status of the client of an allocation.
-}
type ClientStatus
    = ClientPending
    | ClientRunning
    | ClientComplete
    | ClientFailed
    | ClientLost


{-| Decode a client status from JSON.
-}
decoder : Decoder ClientStatus
decoder =
    string
        |> andThen
            (\name ->
                case name of
                    "pending" ->
                        succeed ClientPending

                    "running" ->
                        succeed ClientRunning

                    "complete" ->
                        succeed ClientComplete

                    "failed" ->
                        succeed ClientFailed

                    "lost" ->
                        succeed ClientLost

                    _ ->
                        fail ("Unknown client status " ++ name)
            )
