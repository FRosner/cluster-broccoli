module Templates.Messages exposing (..)

import Http
import Templates.Models exposing (..)

type Msg
    = FetchTemplates (Result Http.Error (List Template))
    | ShowNewInstanceForm Template
    | HideNewInstanceForm Template
