module Models.Resources.InstanceCreationSuccess exposing (InstanceCreationSuccess, decoder)

import Json.Decode as Decode exposing (field)

import Models.Resources.InstanceCreation as InstanceCreation exposing (InstanceCreation)
import Models.Resources.Instance as Instance exposing (Instance)

type alias InstanceCreationSuccess =
  { instanceCreation : InstanceCreation
  , instance : Instance
  }

decoder : Decode.Decoder InstanceCreationSuccess
decoder =
  Decode.map2 InstanceCreationSuccess
    (field "instanceCreation" InstanceCreation.decoder)
    (field "instanceWithStatus" Instance.decoder)
