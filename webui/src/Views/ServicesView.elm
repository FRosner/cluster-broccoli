module Views.ServicesView exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Models.Resources.Service exposing (Service)
import Models.Resources.ServiceStatus exposing (..)
import Utils.HtmlUtils exposing (icon, iconButton, iconButtonText)


view : List Service -> List (Html msg)
view services =
    if List.isEmpty services then
        [ text "-" ]

    else
        List.concatMap serviceView services


serviceView : Service -> List (Html msg)
serviceView service =
    let
        ( iconClass, textColor ) =
            case service.status of
                ServicePassing ->
                    ( "fa fa-check-circle", "#070" )

                ServiceFailing ->
                    ( "fa fa-times-circle", "#900" )

                ServiceUnknown ->
                    ( "fa fa-question-circle", "grey" )
    in
    [ a
        (List.append
            (if service.status /= ServiceUnknown then
                [ href
                    (String.concat
                        [ service.protocol
                        , "://"
                        , service.address
                        , ":"
                        , toString service.port_
                        ]
                    )
                ]

             else
                []
            )
            [ style
                [ ( "margin-right", "8px" )
                , ( "color", textColor )
                ]
            , target "_blank"
            ]
        )
        [ icon iconClass [ style [ ( "margin-right", "4px" ) ] ]
        , text service.name
        ]
    , text " "
    ]
