module Models.Resources.Allocation exposing (..)


type alias AllocationId =
    String


{-| Get the short ID of an allocation.

Nomad uses UUIDs as allocation IDs and allows to refer to allocations with a
short ID, ie, the first component of the UUID ("up to the first dash").

-}
shortAllocationId : AllocationId -> AllocationId
shortAllocationId id =
    String.split "-" id |> List.head |> Maybe.withDefault id
