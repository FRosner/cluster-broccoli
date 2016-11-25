module About.Messages exposing (..)

import Http
import About.Models exposing (AboutInfo)

type Msg
    = FetchAbout (Result Http.Error (AboutInfo))
