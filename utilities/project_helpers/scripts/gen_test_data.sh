#!/usr/bin/env bash
set -o errexit

TARGET_DIR=$1
N=$2
SCRIPT_DIR=$(dirname $(readlink -e $0))

mkdir -p $TARGET_DIR
cd $TARGET_DIR
MAPPING_FILE=narrator2interviews.txt
> $MAPPING_FILE

COUNTER=0
gen_item() {
    COUNTER=$((COUNTER+1))
    i=$COUNTER
    type=$1
    mkdir $i
    pushd $i
    python $SCRIPT_DIR/gen_test_data.py $type
    for x in `ls *.xml`;do
        xmllint --format $x > $x.tmp
        mv $x.tmp $x
    done
    echo "The real license would be here." > license.txt
    echo -e "rozhovor.mpg\tbundle:ORIGINAL\tprimary:true\tdescription:interview" > contents
    echo -e "prepis.txt\tbundle:ORIGINAL\tdescription:transcript" >> contents
    echo -e "souhlas.doc\tbundle:ORIGINAL\tdescription:consent" >> contents
    echo -e "photo.jpg\tbundle:ORIGINAL\tdescription:material" >> contents
    echo -e "thesis.pdf\tbundle:ORIGINAL\tdescription:output" >> contents
    echo -e "license.txt\tbundle:LICENSE" >> contents
    for f in rozhovor.mpg prepis.txt souhlas.doc photo.jpg thesis.pdf;do
        touch $f
    done
    popd
}

for i in `seq 1 $N`;do
    gen_item narrator
    mapping="${COUNTER}:"
    N_INTERVIEWS=`shuf -i0-3 -n1`
    for j in `seq 1 $N_INTERVIEWS`; do
        gen_item interview
        mapping+="${COUNTER},"
    done
    echo "$mapping" | sed -e 's/,$//' >> $MAPPING_FILE
done