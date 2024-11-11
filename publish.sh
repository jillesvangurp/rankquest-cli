#! /usr/bin/env bash
set -e

die () {
    echo >&2 "$@"
    exit 1
}

[ "$#" -eq 1 ] || die "1 argument required, $# provided"
echo $1 | grep -E -q '^[0-9]+\.[0-9]+(\.[0-9]+)?.*?$' || die "Semantic Version argument required, $1 provided"

[[ -z $(git status -s) ]] || die "git status is not clean"

# Check for unpushed commits
if [ -n "$(git log origin/$(git rev-parse --abbrev-ref HEAD)..HEAD)" ]; then
    die "There are unpushed commits"
fi

export TAG=$1

gradle -Pversion="$TAG" publish

#echo "upload to gcloud"
#gsutil -m rsync -r localRepo gs://mvn-public-tryformation/releases

echo "tagging"
git tag "$TAG"

echo "publishing $TAG"

git push --tags

docker build . -t jillesvangurp/rankquest-cli:v"$TAG"
docker build . -t jillesvangurp/rankquest-cli:latest
docker push jillesvangurp/rankquest-cli:v$TAG
docker push jillesvangurp/rankquest-cli:latest
