docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $1
docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}} {{$p}} -> {{(index $conf 0).HostPort}} {{end}}' $1
