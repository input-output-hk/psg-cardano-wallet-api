package iog.psg.cardano.experimental.cli.model


trait PolicySyntax {

  implicit def toPolicyOps(policy: Policy): PolicyOps =
    new PolicyOps(policy)
}
