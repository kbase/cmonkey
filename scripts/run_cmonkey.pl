use strict;
use Data::Dumper;
use Carp;

=head1 NAME

    run_cmonkey - build bi-cluster network starting with expression data series stored in workspace

=head1 SYNOPSIS

    run_cmonkey [--url=http://140.221.85.173:7078/ --ws=<workspace name> --input=<expression data series reference> --genome=<genome reference> --motifs --networks --operons=<operons data reference> --string=<STRING data reference>]

=head1 DESCRIPTION

    Discovers co-regulated modules, or biclusters in gene expression profiles.

=head2 Documentation for underlying call

    Returns Job object ID that will keep workspace reference to CmonkeyRunResult object stored in workspace when the run will be finished.

=head1 OPTIONS

=over 6

=item B<--url>=I<http://140.221.85.173:7078/>
    the service url 

=item B<-h> B<--help>
    print help information

=item B<--version>
    print version information

=item B<--ws>
    workspace name where run result will be stored

=item B<--input>
    Workspace reference of the expression data series

=item B<--genome>
    Workspace reference of genome

=item B<--motifs>
    Motif scoring will be used

=item B<--networks>
    Network scoring will be used

=item B<--operons>
    Workspace reference of operons data set

=item B<--string>
    Workspace reference of STRING data set

=back

=head1 EXAMPLE

    run_cmonkey --url=http://140.221.85.173:7078/ --ws=AKtest --input="AKtest/Halobacterium_sp_NRC1_series" --genome="AKtest/kb|genome.9" --motifs --networks --operons="AKtest/kb|interactionset.8" --string="AKtest/kb|interactionset.7"
    run_cmonkey --help
    run_cmonkey --version

=head1 VERSION

    1.0

=cut

use Getopt::Long;
use Bio::KBase::cmonkey::Client;
use Config::Simple;
use Bio::KBase::Auth;
use Bio::KBase::AuthToken;
use Bio::KBase::AuthUser;

my $usage = "Usage: run_cmonkey [--url=http://140.221.85.173:7078/ --ws=<workspace name> --input=<expression data series reference> --genome=<genome reference> --motifs --networks --operons=<operons data reference> --string=<STRING data reference>]\n";

my $url        = "http://140.221.85.173:7078/";
my $ws         = "";
my $input      = "";
my $genome     = "";
my $motifs     = 0;
my $networks   = 0;
my $operons    = "null";
my $string     = "null";
my $help       = 0;
my $version    = 0;

GetOptions("help"       => \$help,
           "version"    => \$version,
           "ws=s"    => \$ws,
           "input=s"    => \$input,
           "genome=s"    => \$genome,
           "motifs"    => \$motifs,
           "networks"    => \$networks,
           "operons:s"    => \$operons,
           "string:s"    => \$string,
           "url=s"     => \$url) 
           or exit(1);

if($help){
print "NAME\n";
print "run_cmonkey - This command discovers co-regulated modules, or biclusters in gene expression profiles stored in workspace.\n";
print "\n";
print "\n";
print "VERSION\n";
print "1.0\n";
print "\n";
print "SYNOPSIS\n";
print "run_cmonkey [--url=http://140.221.85.173:7078/ --ws=<workspace name> --input=<expression data series reference> --genome=<genome reference> --motifs --networks --operons=<operons data reference> --string=<STRING data reference>]\n";
print "\n";
print "DESCRIPTION\n";
print "INPUT:            This command requires the URL of the service, workspace name, and run parameters.\n";
print "\n";
print "OUTPUT:           This command returns Job object ID.\n";
print "\n";
print "PARAMETERS:\n";
print "--url             The URL of the service, --url=http://140.221.85.173:7078/, required.\n";
print "\n";
print "--ws              Workspace name where cMonkey run result will be stored, required.\n";
print "\n";
print "--input           Workspace reference of expression data series, required.\n";
print "\n";
print "--genome          Workspace reference of genome, required.\n";
print "\n";
print "--motifs          Motif scoring will be used.\n";
print "\n";
print "--networks        Network scoring will be used.\n";
print "\n";
print "--operons         Workspace reference of operons data set.\n";
print "\n";
print "--string          Workspace reference of STRING data set.\n";
print "\n";
print "--help            Display help message to standard out and exit with error code zero; \n";
print "                  ignore all other command-line arguments.  \n";
print "--version         Print version information. \n";
print "\n";
print " \n";
print "EXAMPLES \n";
print "run_cmonkey --url=http://140.221.85.173:7078/ --ws=AKtest --input=\"AKtest/Halobacterium_sp_NRC1_series\" --genome=\"AKtest/kb|genome.9\" --motifs --networks --operons=\"AKtest/kb|interactionset.8\" --string=\"AKtest/kb|interactionset.7\"\n";
print "\n";
print "This command will return a Job object ID.\n";
print "\n";
print "\n";
print "Report bugs to aekazakov\@lbl.gov\n";
exit(0);
};

if($version)
{
    print "run_cmonkey\n";
    print "Copyright (C) 2013 DOE Systems Biology Knowledgebase\n";
    print "License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>.\n";
    print "This is free software: you are free to change and redistribute it.\n";
    print "There is NO WARRANTY, to the extent permitted by law.\n";
    print "\n";
    print "Report bugs to aekazakov\@lbl.gov\n";
    exit(0);
};

unless (@ARGV == 0){
    print $usage;
    exit(1);
};

my $token='';
my $user="";
my $pw="";
my $auth_user = Bio::KBase::AuthUser->new();
my $kbConfPath = $Bio::KBase::Auth::ConfPath;

if (defined($ENV{KB_RUNNING_IN_IRIS})) {
        $token = $ENV{KB_AUTH_TOKEN};
} elsif ( -e $kbConfPath ) {
        my $cfg = new Config::Simple($kbConfPath);
        $user = $cfg->param("authentication.user_id");
        $pw = $cfg->param("authentication.password");
        $cfg->close();
        $token = Bio::KBase::AuthToken->new( user_id => $user, password => $pw);
        $auth_user->get( token => $token->token );
}

if ($token->error_message){
	print $token->error_message."\n\n";
	exit(1);
};


my $cmonkey_run_parameters = {

    "series_ref"=>$input,
    "genome_ref"=>$genome,
    "operome_ref"=>$operons,
    "network_ref"=>$string,
    "networks_scoring"=>$networks,
    "motifs_scoring"=>$motifs
};

my $obj = {
	method => "Cmonkey.run_cmonkey",
	params => [$ws, $cmonkey_run_parameters],
};

my $client = Bio::KBase::cmonkey::Client::RpcClient->new;
$client->{token} = $token->token;

my $result = $client->call($url, $obj);

my @keys = keys % { $result };

if (${$result}{is_success} == 1){
	my $result_id = ${$result}{jsontext};
	$result_id =~ s/\"\]\}$//;
	$result_id =~ s/^.*\"\,\"result\"\:\[\"//;
	print $result_id."\n\n";
	exit(0);
}
else {
	print ${$result}{jsontext}."\n";
	exit(1);
}
exit(1);

