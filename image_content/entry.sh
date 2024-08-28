#!/bin/bash -x
java -cp /pmfilebench/iotest.jar ${CNIV_ARGS} ${ADD_ARGS} -Diotest.length=${TEST_LENGHT} $1 ${MODE} ${MODE_ARGS}
