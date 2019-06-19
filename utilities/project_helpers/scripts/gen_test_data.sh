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
add_contents(){
    flip=$(shuf -i0-1 -n1)
    if [ $flip -gt 0 ]; then
        content_line=$1
        filename=`echo -e $content_line | cut -f1`
        echo -e $content_line >> contents
        touch $filename
        python $SCRIPT_DIR/gen_test_data.py bitstream $filename
    fi

}

gen_item() {
    COUNTER=$((COUNTER+1))
    i=$COUNTER
    type=$1
    mkdir $i
    pushd $i
    python $SCRIPT_DIR/gen_test_data.py $type
    echo "The real license would be here." > license.txt
    echo -e "license.txt\tbundle:LICENSE" > contents
    if [ "$type" = "narrator" ]; then
        add_contents "souhlas.doc\tbundle:ORIGINAL\tdescription:consent"
        add_contents "photo.jpg\tbundle:ORIGINAL\tdescription:material"
        add_contents "thesis.pdf\tbundle:ORIGINAL\tdescription:output"
    elif [ "$type" = "interview" ]; then
        add_contents "rozhovor.mpg\tbundle:ORIGINAL\tprimary:true\tdescription:interview"
        add_contents "prepis.txt\tbundle:ORIGINAL\tdescription:transcript"
    fi
    for x in `ls *.xml`;do
        xmllint --format $x > $x.tmp
        mv $x.tmp $x
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