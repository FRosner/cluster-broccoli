module Models.Resources.InstanceCreation exposing (InstanceCreation, encoder)

import Json.Encode as Encode
import Dict exposing (Dict)

import Models.Resources.ServiceStatus as ServiceStatus exposing (ServiceStatus)

type alias InstanceCreation =
  { templateId : String
  , parameters : Dict String String
  }

encoder instanceCreation =
  Encode.object
    [ ("templateId", Encode.string instanceCreation.templateId)
    , ("parameters", (parametersToObject instanceCreation.parameters))
    ]

parametersToObject parameters =
  Encode.object
    ( parameters
      |> Dict.toList
      |> List.map (\(k, v) -> (k, Encode.string v))
    )
