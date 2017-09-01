module Models.Resources.InstanceTasks exposing (..)

import Models.Resources.Instance exposing (InstanceId)
import Models.Resources.AllocatedTask as AllocatedTask exposing (AllocatedTask)
import Json.Decode exposing (Decoder, string, field, map2, list)


{-| The tasks of an instance
-}
type alias InstanceTasks =
    { instanceId : InstanceId
    , allocatedTasks : List AllocatedTask
    }


{-| Decode tasks of an instance from JSON.
-}
decoder : Decoder InstanceTasks
decoder =
    map2 InstanceTasks
        (field "instanceId" string)
        (field "allocatedTasks" (list AllocatedTask.decoder))
