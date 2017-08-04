module Models.Resources.Allocation exposing (..)

import Json.Decode as Decode exposing (field)
import Models.Resources.TaskState as TaskState exposing (TaskState)
import Models.Resources.ClientStatus as ClientStatus exposing (ClientStatus)


{-| The type for the ID of an allocation.
-}
type alias AllocationId =
    String


{-| An allocation of a task with a state.
-}
type alias Allocation =
    { id : AllocationId
    , clientStatus : ClientStatus
    , taskState : TaskState
    }


{-| Decode an allocation from JSON.
-}
decoder : Decode.Decoder Allocation
decoder =
    Decode.map3 Allocation
        (field "id" Decode.string)
        (field "clientStatus" ClientStatus.decoder)
        (field "taskState" TaskState.decoder)
