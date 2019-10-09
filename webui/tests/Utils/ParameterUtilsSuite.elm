module Utils.ParameterUtilsSuite exposing (tests)

import Dict exposing (Dict)
import Expect
import Models.Resources.Template as Template
import Test exposing (Test, describe, test)
import Utils.ParameterUtils exposing (getOtherParametersSorted, zipWithOrderIndex)


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
        , describe "zipWithOrderIndex"
            [ test "assign infinity if no order index is given" <|
                \() ->
                    Expect.equal
                        [ ( 1 / 0, "a" ), ( 1 / 0, "b" ), ( 1 / 0, "c" ) ]
                        (zipWithOrderIndex
                            Dict.empty
                            [ "a", "b", "c" ]
                        )
            , test "zip the order index correctly" <|
                \() ->
                    Expect.equal
                        [ ( 2, "a" ), ( 1, "b" ) ]
                        (zipWithOrderIndex
                            (Dict.fromList
                                [ ( "a"
                                  , { id = "a"
                                    , default = Nothing
                                    , secret = Nothing
                                    , name = Nothing
                                    , orderIndex = Just 2
                                    , dataType = Template.RawParam
                                    }
                                  )
                                , ( "b"
                                  , { id = "b"
                                    , default = Nothing
                                    , secret = Nothing
                                    , name = Nothing
                                    , orderIndex = Just 1
                                    , dataType = Template.RawParam
                                    }
                                  )
                                ]
                            )
                            [ "a", "b" ]
                        )
            ]
        ]
