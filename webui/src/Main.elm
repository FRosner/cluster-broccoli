module Main exposing (main)

import Bootstrap.Grid exposing (container)
import Updates.UpdateErrors exposing (updateErrors)
import Updates.UpdateLoginForm exposing (updateLoginForm)
import Updates.UpdateLoginStatus exposing (updateLoginStatus)
import Updates.UpdateBodyView exposing (updateBodyView)
import Messages exposing (..)
import Views.Header
import Views.Body
import Views.Footer
import Views.Notifications
import Commands.LoginLogout as LoginLogout
import Utils.CmdUtils as CmdUtils
import Routing
import Model exposing (Model)
import Ws
import Html exposing (..)
import Html.Attributes exposing (..)
import Navigation exposing (Location)
import Dict exposing (Dict)
import Bootstrap.Utilities.Spacing as Spacing
import Bootstrap.Tab as Tab
import Bootstrap.Grid as Grid
import Bootstrap.Grid.Row as Row
import Bootstrap.Grid.Col as Col
import Bootstrap.CDN as CDN


init : Location -> ( Model, Cmd AnyMsg )
init location =
    ( Model.initial location (Routing.parseLocation location)
    , CmdUtils.sendMsg AttemptReconnect
    )


update : AnyMsg -> Model -> ( Model, Cmd AnyMsg )
update msg model =
    case msg of
        SendWsMsg message ->
            ( model
            , Ws.send model.location message
            )

        AttemptReconnect ->
            ( model
            , Cmd.map UpdateLoginStatusMsg
                LoginLogout.verifyLogin
            )

        WsConnectError ( url, error ) ->
            let
                l =
                    Debug.log "ConnectError" ( url, error )
            in
                ( { model
                    | wsConnected = False
                    , aboutInfo = Nothing
                    , templates = Dict.empty
                    , instances = Dict.empty
                  }
                , Ws.attemptReconnect
                )

        WsConnect url ->
            let
                l =
                    Debug.log "Connect" url
            in
                ( { model | wsConnected = True }
                , Cmd.none
                )

        WsListenError ( url, error ) ->
            ( model
            , Cmd.none
            )

        WsSuccessDisconnect url ->
            let
                l =
                    Debug.log "SuccessDisconnect" url
            in
                ( { model
                    | wsConnected = False
                    , aboutInfo = Nothing
                    , templates = Dict.empty
                    , instances = Dict.empty
                  }
                , Cmd.none
                )

        WsErrorDisconnect ( url, error ) ->
            let
                l =
                    Debug.log "ErrorDisconnect" ( url, error )
            in
                ( model
                , Cmd.none
                )

        WsConnectionLost url ->
            let
                l =
                    Debug.log "ConnectionLost" url
            in
                ( { model
                    | wsConnected = False
                    , aboutInfo = Nothing
                    , templates = Dict.empty
                    , instances = Dict.empty
                  }
                , Ws.connect model.location
                )

        WsMessage ( url, message ) ->
            Ws.update message model

        WsSendError ( url, message, error ) ->
            ( model
            , Cmd.none
            )

        WsSent ( url, message ) ->
            ( model
            , Cmd.none
            )

        UpdateLoginStatusMsg subMsg ->
            updateLoginStatus subMsg model

        UpdateErrorsMsg subMsg ->
            let
                ( newErrors, cmd ) =
                    updateErrors subMsg model.errors
            in
                ( { model | errors = newErrors }
                , cmd
                )

        UpdateBodyViewMsg subMsg ->
            let
                ( newBodyUiModel, cmd ) =
                    updateBodyView subMsg model.bodyUiModel
            in
                ( { model | bodyUiModel = newBodyUiModel }
                , cmd
                )

        TabMsg state ->
            ( { model | tabState = state }, Cmd.none )

        UpdateLoginFormMsg subMsg ->
            let
                ( newLoginForm, cmd ) =
                    updateLoginForm subMsg model.loginForm
            in
                ( { model | loginForm = newLoginForm }
                , cmd
                )

        OnLocationChange location ->
            let
                newRoute =
                    Routing.parseLocation location
            in
                ( { model
                    | route = newRoute
                    , location = location
                  }
                , Cmd.none
                )

        TemplateFilter filterString ->
            ( { model | templateFilter = filterString }
            , Cmd.none
            )

        InstanceFilter filterString ->
            ( { model | instanceFilter = filterString }
            , Cmd.none
            )

        NoOp ->
            ( model, Cmd.none )


gridView : List (Html msg) -> Html msg
gridView pageContent =
    Grid.container []
        [ Grid.row []
            [ Grid.col []
                pageContent
            ]
        ]


view : Model -> Html AnyMsg
view model =
    let
        maybeAuthEnabled =
            Maybe.map (\i -> i.authInfo.enabled) model.aboutInfo

        mainView =
            if
                (model.authRequired
                    == Just True
                    || model.authRequired
                    == Nothing
                    || (model.authRequired
                            == Just False
                            && maybeAuthEnabled
                            == Nothing
                       )
                )
            then
                div [] []
            else
                Tab.config TabMsg
                    |> Tab.center
                    |> Tab.items
                        [ Tab.item
                            { id = "instances_view"
                            , link = Tab.link [] [ h5 [] [ text "Instances View" ] ]
                            , pane =
                                Tab.pane [ Spacing.mt3 ]
                                    [ Html.map
                                        UpdateBodyViewMsg
                                        (Views.Body.view
                                            (Dict.filter (\k v -> String.contains model.templateFilter k) model.templates)
                                            (Dict.filter (\k v -> String.contains model.instanceFilter k) model.instances)
                                            model.tasks
                                            model.bodyUiModel
                                            (Maybe.map (\i -> i.authInfo.userInfo.role) model.aboutInfo)
                                        )
                                    ]
                            }
                        , Tab.item
                            { id = "resources_view"
                            , link = Tab.link [] [ h5 [] [ text "Resources View" ] ]
                            , pane =
                                Tab.pane [ Spacing.mt3 ]
                                    [ h4 [] [ text "Tab 2 Heading" ]
                                    , p [] [ text "This is something completely different." ]
                                    ]
                            }
                        ]
                    |> Tab.view model.tabState
    in
        gridView
            [ Views.Header.view model.aboutInfo model.loginForm model.authRequired model.templateFilter model.instanceFilter
            , Views.Notifications.view model.errors
            , mainView

            -- , text (toString model) -- enable this for a debug view of the whole model
            , Views.Footer.view model.aboutInfo model.wsConnected
            ]



-- Sub.map UpdateErrorsMsg ( WebSocket.listen "ws://localhost:9000/ws" AddError ) when AddError is an UpdateErrorsMsg


subscriptions : Model -> Sub AnyMsg
subscriptions model =
    Ws.listen model.location


main : Program Never Model AnyMsg
main =
    Navigation.program OnLocationChange
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        }
