module Templates.Commands exposing (..)

import Http
import Json.Decode as Decode exposing (field)
import Templates.Models exposing (..)
import Templates.Messages exposing (..)


fetch : Cmd Msg
fetch =
    Http.get fetchUrl templatesDecoder
        |> Http.send FetchTemplates

fetchUrl : String
fetchUrl =
    "http://localhost:9000/api/v1/templates"

templatesDecoder : Decode.Decoder (Templates)
templatesDecoder =
    Decode.list templateDecoder

templateDecoder =
  Decode.map3 Template
    (field "id" Decode.string)
    (field "description" Decode.string)
    (field "version" Decode.string)
