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


{-| Get the short ID of an allocation.

Nomad uses UUIDs as allocation IDs and allows to refer to allocations with a
short ID, ie, the first component of the UUID ("up to the first dash").

-}
shortAllocationId : AllocationId -> AllocationId
shortAllocationId id =
    String.split "-" id |> List.head |> Maybe.withDefault id


{-| Decode an allocation from JSON.
-}
decoder : Decode.Decoder Allocation
decoder =
    Decode.map3 Allocation
        (field "id" Decode.string)
        (field "clientStatus" ClientStatus.decoder)
        (field "taskState" TaskState.decoder)
