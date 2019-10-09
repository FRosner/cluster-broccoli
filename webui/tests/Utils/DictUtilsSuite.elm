module Utils.DictUtilsSuite exposing (tests)

import Dict exposing (Dict)
import Expect
import Test exposing (Test, describe, test)
import Utils.DictUtils exposing (flatMap, flatten)


tests : Test
tests =
    describe "DictUtils"
        [ describe "flatten"
            [ test "works as expected" <|
                \() ->
                    Expect.equal
                        (Dict.fromList [ ( "a", "a" ), ( "c", "c" ) ])
                        (flatten <| Dict.fromList [ ( "a", Just "a" ), ( "b", Nothing ), ( "c", Just "c" ) ])
            , test "return empty map for a Map of Nothings" <|
                \() ->
                    Expect.equal
                        Dict.empty
                        (flatten <| Dict.fromList [ ( "a", Nothing ), ( "b", Nothing ), ( "c", Nothing ) ])
            ]
        , describe "flatMap"
            [ test "maps value as expected" <|
                \() ->
                    Expect.equal
                        (Dict.fromList [ ( "a", 2 ), ( "c", 5 ) ])
                        (Dict.fromList [ ( "a", Just "Hi" ), ( "b", Nothing ), ( "c", Just "Demon" ) ]
                            |> flatMap mapStringtoLen
                        )
            , test "return empty map for a Map of Nothings" <|
                \() ->
                    Expect.equal
                        Dict.empty
                        (Dict.fromList [ ( "a", Nothing ), ( "b", Nothing ), ( "c", Nothing ) ] |> flatMap mapStringtoLen)
            ]
        ]


mapStringtoLen comparable maybeString =
    Maybe.andThen (\s -> Just (String.length s))
        maybeString
