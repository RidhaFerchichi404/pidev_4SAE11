Param(
    [string]$Namespace = "smart-freelance",
    [string]$ImageRepo = "docker.io/ridhaferchichi",
    [string]$ImageTag = "latest",
    [switch]$SkipIngress
)

$ErrorActionPreference = "Stop"

python scripts/render-k8s-secrets.py --repo-root . --mdp-file mdp.local --output k8s/02-secrets.generated.yaml --namespace $Namespace

$renderDir = ".k8s-rendered"
if (Test-Path $renderDir) { Remove-Item -Recurse -Force $renderDir }
New-Item -ItemType Directory -Path $renderDir | Out-Null
Copy-Item -Recurse -Path "k8s/*" -Destination $renderDir
Remove-Item -Recurse -Force "$renderDir/monitoring" -ErrorAction SilentlyContinue

$files = Get-ChildItem -Path $renderDir -Filter "*.yaml" -File
foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $content = $content -replace "YOUR_DOCKERHUB_USERNAME/([A-Za-z0-9._-]+)(:[A-Za-z0-9._-]+)?", "$ImageRepo/`$1:$ImageTag"
    $content = $content -replace "namespace:\s*smart-freelance", "namespace: $Namespace"
    Set-Content -Path $f.FullName -Value $content -NoNewline
}

kubectl apply -f "$renderDir/00-namespace.yaml"
kubectl apply -f "$renderDir/01-configmap.yaml"
kubectl apply -f "$renderDir/02-secrets.generated.yaml"
kubectl apply -f "$renderDir/03-mysql.yaml"
kubectl apply -f "$renderDir/04-keycloak.yaml"
kubectl apply -f "$renderDir/05-eureka.yaml"
kubectl apply -f "$renderDir/06-config-server.yaml"
kubectl apply -f "$renderDir/07-api-gateway.yaml"
kubectl apply -f "$renderDir/12-ollama.yaml"
kubectl apply -f "$renderDir/08-microservices.yaml"
kubectl apply -f "$renderDir/09-frontend.yaml"
if (-not $SkipIngress) {
    kubectl apply -f "$renderDir/10-ingress.yaml"
}

kubectl -n $Namespace rollout status deployment/ollama --timeout=300s
foreach ($d in @("eureka","config-server","keycloak-auth","aimodel","planning","notification","api-gateway","frontend")) {
    kubectl -n $Namespace rollout status deployment/$d --timeout=300s
}

kubectl -n $Namespace get deploy,pods,svc
