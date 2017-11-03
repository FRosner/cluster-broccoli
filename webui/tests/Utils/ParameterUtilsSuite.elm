module Utils.ParameterUtilsSuite exposing (tests)

import Utils.ParameterUtils exposing (getOtherParametersSorted)
import Test exposing (test, describe, Test)
import Expect


tests : Test
tests =
    describe "ParameterUtils"
        [ describe "getOtherParametersSorted"
            [ test "removes id parameter" <|
                \() ->
                    Expect.equal
                        [ "a", "b" ]
                        (getOtherParametersSorted [ ( 1, "a" ), ( 2, "b" ), ( 3, "id" ) ])
            , test "sorts based on the sort index" <|
                \() ->
                    Expect.equal
                        [ "b", "a" ]
                        (getOtherParametersSorted [ ( 2, "a" ), ( 1, "b" ) ])
            , test "sorts based on the parameter if the index is the same" <|
                \() ->
                    Expect.equal
                        [ "b", "a", "c" ]
                        (getOtherParametersSorted [ ( 2, "c" ), ( 2, "a" ), ( 1, "b" ) ])
            , test "sorts within the same index ignoring case" <|
                \() ->
                    Expect.equal
                        [ "bar", "FOO" ]
                        (getOtherParametersSorted [ ( 1, "FOO" ), ( 1, "bar" ) ])
            ]
        ]
