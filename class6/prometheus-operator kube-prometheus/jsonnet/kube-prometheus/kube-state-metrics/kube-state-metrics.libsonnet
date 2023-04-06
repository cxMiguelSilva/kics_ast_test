local kubeRbacProxyContainer = import '../kube-rbac-proxy/containerMixin.libsonnet';
local ksm = import 'github.com/kubernetes/kube-state-metrics/jsonnet/kube-state-metrics/kube-state-metrics.libsonnet';

{
  _config+:: {
    versions+:: {
      kubeStateMetrics: '1.9.7',
    },
    imageRepos+:: {
      kubeStateMetrics: 'quay.io/coreos/kube-state-metrics',
    },
    kubeStateMetrics+:: {
      scrapeInterval: '30s',
      scrapeTimeout: '30s',
      labels: {
        'app.kubernetes.io/name': 'kube-state-metrics',
        'app.kubernetes.io/version': $._config.versions.kubeStateMetrics,
        'app.kubernetes.io/component': 'exporter',
        'app.kubernetes.io/part-of': 'kube-prometheus',
      },
      selectorLabels: {
        [labelName]: $._config.kubeStateMetrics.labels[labelName]
        for labelName in std.objectFields($._config.kubeStateMetrics.labels)
        if !std.setMember(labelName, ['app.kubernetes.io/version'])
      },
    },
  },
  kubeStateMetrics+::
    ksm {
      local version = self.version,
      name:: 'kube-state-metrics',
      namespace:: $._config.namespace,
      version:: $._config.versions.kubeStateMetrics,
      image:: $._config.imageRepos.kubeStateMetrics + ':v' + $._config.versions.kubeStateMetrics,
      commonLabels:: $._config.kubeStateMetrics.labels,
      podLabels:: $._config.kubeStateMetrics.selectorLabels,
      service+: {
        spec+: {
          ports: [
            {
              name: 'https-main',
              port: 8443,
              targetPort: 'https-main',
            },
            {
              name: 'https-self',
              port: 9443,
              targetPort: 'https-self',
            },
          ],
        },
      },
      deployment+: {
        spec+: {
          template+: {
            spec+: {
              containers: std.map(function(c) c {
                ports:: null,
                livenessProbe:: null,
                readinessProbe:: null,
                args: ['--host=127.0.0.1', '--port=8081', '--telemetry-host=127.0.0.1', '--telemetry-port=8082'],
              }, super.containers),
            },
          },
        },
      },
      serviceMonitor:
        {
          apiVersion: 'monitoring.coreos.com/v1',
          kind: 'ServiceMonitor',
          metadata: {
            name: 'kube-state-metrics',
            namespace: $._config.namespace,
            labels: $._config.kubeStateMetrics.labels,
          },
          spec: {
            jobLabel: 'app.kubernetes.io/name',
            selector: { matchLabels: $._config.kubeStateMetrics.selectorLabels },
            endpoints: [
              {
                port: 'https-main',
                scheme: 'https',
                interval: $._config.kubeStateMetrics.scrapeInterval,
                scrapeTimeout: $._config.kubeStateMetrics.scrapeTimeout,
                honorLabels: true,
                bearerTokenFile: '/var/run/secrets/kubernetes.io/serviceaccount/token',
                relabelings: [
                  {
                    regex: '(pod|service|endpoint|namespace)',
                    action: 'labeldrop',
                  },
                ],
                tlsConfig: {
                  insecureSkipVerify: true,
                },
              },
              {
                port: 'https-self',
                scheme: 'https',
                interval: $._config.kubeStateMetrics.scrapeInterval,
                bearerTokenFile: '/var/run/secrets/kubernetes.io/serviceaccount/token',
                tlsConfig: {
                  insecureSkipVerify: true,
                },
              },
            ],
          },
        },
    } +
    (kubeRbacProxyContainer {
       config+:: {
         kubeRbacProxy: {
           local cfg = self,
           image: $._config.imageRepos.kubeRbacProxy + ':' + $._config.versions.kubeRbacProxy,
           name: 'kube-rbac-proxy-main',
           securePortName: 'https-main',
           securePort: 8443,
           secureListenAddress: ':%d' % self.securePort,
           upstream: 'http://127.0.0.1:8081/',
           tlsCipherSuites: $._config.tlsCipherSuites,
         },
       },
     }).deploymentMixin +
    (kubeRbacProxyContainer {
       config+:: {
         kubeRbacProxy: {
           local cfg = self,
           image: $._config.imageRepos.kubeRbacProxy + ':' + $._config.versions.kubeRbacProxy,
           name: 'kube-rbac-proxy-self',
           securePortName: 'https-self',
           securePort: 9443,
           secureListenAddress: ':%d' % self.securePort,
           upstream: 'http://127.0.0.1:8082/',
           tlsCipherSuites: $._config.tlsCipherSuites,
         },
       },
     }).deploymentMixin,
}