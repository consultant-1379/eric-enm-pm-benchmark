{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-enm-pm-bench.global" }}
  {{- $globalDefaults := dict "cnivAgent" (dict "enabled" false) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "cnivAgent" (dict "endpoint" "eric-cniv-agent:8080" )) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "url" "armdocker.rnd.ericsson.se")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "pullSecret" "") -}}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-enm-pm-bench.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
benchmark name
*/}}
{{- define "eric-enm-pm-bench.namelabel" -}}
  benchmarkName: {{ include "eric-enm-pm-bench.name" . | quote }}
{{- end }}

{{/*
benchmark group
*/}}
{{- define "eric-enm-pm-bench.grouplabel" -}}
  benchmarkgroup: {{ include "eric-enm-pm-bench.benchmarkGroup.label" . | quote }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-enm-pm-bench.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The mainImage path (DR-D1121-067)
*/}}
{{- define "eric-enm-pm-bench.iotestImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := $productInfo.images.iotest.registry -}}
    {{- $repoPath := $productInfo.images.iotest.repoPath -}}
    {{- $name := $productInfo.images.iotest.name -}}
    {{- $tag := $productInfo.images.iotest.tag -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if .Values.imageCredentials.iotest -}}
            {{- if .Values.imageCredentials.iotest.registry -}}
                {{- if .Values.imageCredentials.iotest.registry.url -}}
                    {{- $registryUrl = .Values.imageCredentials.iotest.registry.url -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.imageCredentials.iotest.repoPath) -}}
                {{- $repoPath = .Values.imageCredentials.iotest.repoPath -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- if .Values.images -}}
        {{- if .Values.images.iotest -}}
            {{- $name = .Values.images.iotest.name }}
            {{- $tag = .Values.images.iotest.tag }}
        {{- end -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Registry URL
*/}}
{{- define "eric-enm-pm-bench.registry" -}}
{{- $registr := .Values.imageCredentials.initBench.registry.url }}
{{- if .Values.global -}}
    {{- if .Values.global.registry -}}
        {{- if .Values.global.registry.url -}}
            {{- $registr = .Values.global.registry.url -}}
        {{- end }}
    {{- end }}
{{- end }}
{{- $registr -}}
{{- end }}

{{/*
The Init Image path
*/}}
{{- define "eric-enm-pm-bench.initbenchImagePath" -}}
{{  include "eric-enm-pm-bench.registry" . }}/{{ .Values.imageCredentials.initBench.repoPath }}/{{ .Values.images.initBench.name }}:{{ .Values.images.initBench.tag }}
{{- end }}

{{/*
Define PullSecret
*/}}
{{- define "eric-enm-pm-bench.pullSecret" -}}
    {{- $pullSecret := .Values.imageCredentials.initBench.registry.pullSecret -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.pullSecret -}}
                {{- $pullSecret = .Values.global.registry.pullSecret }}
            {{- end }}
        {{- end }}
    {{- end }}
{{- $pullSecret -}}
{{- end }}


{{/*
cnivAgent switch
*/}}
{{- define "eric-enm-pm-bench.cnivAgent" -}}
{{- $g := fromJson (include "eric-enm-pm-bench.global" .) -}}
{{- $g.cnivAgent.enabled }}
{{- end -}}

{{/*
cnivAgent address
*/}}
{{- define "eric-enm-pm-bench.cnivAgent.endpoint" -}}
{{- $g := fromJson (include "eric-enm-pm-bench.global" .) -}}
{{- $endpoint := $g.cnivAgent.endpoint }}
{{- if .Values.global }}
  {{- if .Values.global.cnivAgent }}
    {{- if .Values.global.cnivAgent.enabled }}
      {{- $cnivname := .Values.global.cnivAgent.name }}
      {{- $cnivport := .Values.global.cnivAgent.port }}
      {{- $endpoint := printf "%s:%v" $cnivname $cnivport }}
      {{- $endpoint | trunc 63 | trimSuffix "-" }}
    {{- end }}
  {{- end }}
{{- else }}
  {{- $g.cnivAgent.endpoint }}
{{- end -}}
{{- end -}}


{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "eric-enm-pm-bench.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-enm-pm-bench.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}


{{/*
*
* Labels
*
*/}}

{{/*
Standard labels of Helm and Kubernetes
*/}}
{{- define "eric-enm-pm-bench.standard-labels" }}
app: {{ template "eric-enm-pm-bench.name" .}}
chart: {{ template "eric-enm-pm-bench.chart" . }}
release: {{ .Release.Name }}
heritage: {{ .Release.Service }}
app.kubernetes.io/name: {{ template "eric-enm-pm-bench.name" . }}
app.kubernetes.io/version: {{ template "eric-enm-pm-bench.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
sidecar.istio.io/inject: "false"
{{- end }}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{- define "eric-enm-pm-bench.config-labels" }}
{{- $global := (.Values.global).labels -}}
{{- $service := .Values.labels -}}
{{- include "eric-enm-pm-bench.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end -}}

{{/*
Set the benchmark config labels
*/}}
{{- define "eric-enm-pm-bench.benchmarkGroup.label" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.cnivAgent }}
      {{- if .Values.global.cnivAgent.enabled }}
        {{- range $groupmap := .Values.global.sequence -}}
          {{- range $group,$benchmarks := $groupmap -}}
            {{- range $bench := $benchmarks }}
              {{- if eq $.Chart.Name $bench }}
                {{- $label := print $group -}}
                {{- $label | lower | trunc 54 | trimSuffix "-" -}}
              {{- end }}
            {{- end }}
          {{- end }}
        {{- end }}
      {{- end }}
    {{- end }}
  {{- else }}
    {{- $label := print "default" -}}
    {{- $label | lower | trunc 54 | trimSuffix "-" -}}
  {{- end }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config
*/}}
{{- define "eric-enm-pm-bench.helm-labels" }}
{{- $standard := include "eric-enm-pm-bench.standard-labels" . | fromYaml -}}
{{- $config := include "eric-enm-pm-bench.config-labels" . | fromYaml -}}
{{- include "eric-enm-pm-bench.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end }}

{{/*
Merged labels for Default, which includes benchmarkName and benchmarkGroup
*/}}
{{- define "eric-enm-pm-bench.benchmarknamegroup-labels" -}}
  {{- $benchmarkName := include "eric-enm-pm-bench.namelabel" . | fromYaml -}}
  {{- $benchmarkGroup := include "eric-enm-pm-bench.grouplabel" . | fromYaml -}}
  {{- include "eric-enm-pm-bench.mergeLabels" (dict "location" .Template.Name "sources" (list $benchmarkName $benchmarkGroup)) | trim }}
{{- end }}

{{/*
Merged labels for Default, which benchmarkNameGroup-labels and helm-labels
*/}}
{{- define "eric-enm-pm-bench.labels" -}}
  {{- $helmLabels := include "eric-enm-pm-bench.helm-labels" . | fromYaml -}}
  {{- $benchmarkNameGroupLabels := include "eric-enm-pm-bench.benchmarknamegroup-labels" . | fromYaml -}}
  {{- include "eric-enm-pm-bench.mergeLabels" (dict "location" .Template.Name "sources" (list $helmLabels $benchmarkNameGroupLabels)) | trim }}
{{- end }}

{{/*
*
* Annotations
*
*/}}

{{/*
Create annotation for the product information (DR-D1121-064, DR-D1121-067)
*/}}
{{- define "eric-enm-pm-bench.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+].*" .Chart.Version "${1}" }}
{{- end}}

{{/*
Merge config annotations from service and global annotations
*/}}
{{- define "eric-enm-pm-bench.config-annotations" -}}
{{- $global := (.Values.global).annotations -}}
{{- $service := .Values.annotations -}}
{{- include "eric-enm-pm-bench.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end -}}

{{/*
Merge config annotations with productInfo
*/}}
{{- define "eric-enm-pm-bench.configproduct-annotations" -}}
  {{- $productInfo := include "eric-enm-pm-bench.product-info" . | fromYaml -}}
  {{- $configAnnotations := include "eric-enm-pm-bench.config-annotations" . | fromYaml -}}
  {{- include "eric-enm-pm-bench.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $configAnnotations)) | trim }}
{{- end }}

{{/*
Merge configProduct-annotations and Prometheus Annotations
*/}}
{{- define "eric-enm-pm-bench.annotations" -}}
  {{- $productConfigInfo := include "eric-enm-pm-bench.configproduct-annotations" . | fromYaml -}}
  {{- $prometheusAnnotations := include "eric-enm-pm-bench.prometheus.annotations" . | fromYaml -}}
  {{- include "eric-enm-pm-bench.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productConfigInfo $prometheusAnnotations)) | trim }}
{{- end }}

{{/*
Prometheus Annotations 
*/}}
{{- define "eric-enm-pm-bench.prometheus.annotations" -}}
  prometheus.io/port: "{{ .Values.benchmarkArgs.metricsPort }}"
  prometheus.io/scrape: "true"
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-enm-pm-bench.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "eric-enm-pm-bench.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Common security context
*/}}
{{- define "eric-enm-pm-bench.container-securitycontext" -}}
allowPrivilegeEscalation: false
privileged: false
readOnlyRootFilesystem: true
runAsNonRoot: true
capabilities:
  drop:
  - all
{{- end }}

{{/*
Spec security context
*/}}
{{- define "eric-enm-pm-bench.container-specsecuritycontext" -}}
fsGroup: 10000
{{- end }}

{{/*
Common Storageclass name
*/}}
{{- define "eric-enm-pm-bench.pvc-cniv-storageclassname" -}}
    {{- $backendType := .Values.persistentVolumeClaim.targetBackend -}}
    {{- $storageClass := .Values.persistentVolumeClaim.storageClass.name }}
    {{- if .Values.global -}}
        {{- if .Values.global.persistentVolumeClaim -}}
            {{- if .Values.global.persistentVolumeClaim.storageClass -}}
                {{- if eq $backendType "block" -}}
                    {{- if .Values.global.persistentVolumeClaim.storageClass.block -}}
                        {{- $storageClass = .Values.global.persistentVolumeClaim.storageClass.block -}}
                    {{- end }}
                {{- else if eq $backendType "file" -}}
                    {{- if .Values.global.persistentVolumeClaim.storageClass.file -}}
                        {{- $storageClass = .Values.global.persistentVolumeClaim.storageClass.file -}}
                    {{- end }}
                {{- end }}
            {{- end }}
        {{- end }}
    {{- end }}
    {{- $storageClass -}}
{{- end }}

{{/*
set the threads per writer
*/}}
{{- define "eric-enm-pm-bench.threadperwriter" -}}
  -DlowThreads={{- default 75 .Values.benchmarkArgs.writer.threadsPerWriter }}
{{- end }}

{{/*
Create the fsGroup value according to DR-D1123-136.
*/}}
{{- define "eric-enm-pm-bench.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if not (kindIs "invalid" .Values.global.fsGroup.manual) -}}
        {{- if .Values.global.fsGroup.manual | int64 | toString | trimAll " " | mustRegexMatch "^[0-9]+$" }}
          {{- .Values.global.fsGroup.manual | int64 | toString | trimAll " " }}
        {{- else }}
          {{- fail "global.fsGroup.manual shall be a positive integer if given" }}
        {{- end }}
      {{- else -}}
        {{- if eq (.Values.global.fsGroup.namespace | toString) "true" -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
        {{- end -}}
      {{- end -}}
    {{- else -}}
      10000
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Resolve imagePullPolicy
*/}}
{{- define "eric-enm-pm-bench.imagePullPolicy" -}}
  {{- $imagePullPolicy := .Values.imageCredentials.iotest.imagePullPolicy -}}
  {{- if .Values.global -}}
      {{- if .Values.global.registry -}}
          {{- $imagePullPolicy = .Values.global.registry.imagePullPolicy -}}
      {{- end }}
  {{- end }}
  {{- coalesce $imagePullPolicy "IfNotPresent" -}}
{{- end }}
