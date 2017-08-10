module Models.Resources.Task exposing (..)

import Json.Decode as Decode exposing (field, list, string)
import Models.Resources.Allocation as Allocation exposing (Allocation)


{-| A type for the name of a task
-}
type alias TaskName =
    String


{-| A task with a name and the list of its allocations.
-}
type alias Task =
    { name : TaskName
    , allocations : List Allocation
    }


{-| Decode an allocation from JSON.
-}
decoder : Decode.Decoder Task
decoder =
    Decode.map2 Task
        (field "name" string)
        (field "allocations" (list Allocation.decoder))
