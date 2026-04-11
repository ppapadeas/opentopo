# Terms of Service

**Last updated:** April 2026

## License

OpenTopo is free software licensed under the [GNU Affero General Public License v3.0](LICENSE). You may use, modify, and distribute it under the terms of that license.

## Disclaimer of Warranty

OpenTopo is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and noninfringement.

## Accuracy Disclaimer

OpenTopo implements the HEPOS coordinate transformation model published by Ktimatologio S.A. While the implementation has been validated against reference data, **the developers make no guarantee of coordinate accuracy**.

- Transformation accuracy depends on the quality of GNSS input, RTK correction data, and the HEPOS model itself.
- The HEPOS correction grid has stated accuracy of approximately 2-3 cm under ideal conditions. Actual accuracy varies by location and conditions.
- **Do not use OpenTopo as the sole basis for legal surveys, property boundary determinations, or safety-critical applications without independent verification.**
- Always validate results against known control points before relying on them for professional work.

## HEPOS Grid Data

The HEPOS correction grids (`dE_2km_V1-0.grd`, `dN_2km_V1-0.grd`) are published by Ktimatologio S.A. for use by equipment manufacturers and surveyors operating in the Greek market. OpenTopo bundles these grids for end-user convenience. Ktimatologio S.A. retains all rights to this data.

## Limitation of Liability

In no event shall the developers of OpenTopo be liable for any claim, damages, or other liability arising from the use of the software, including but not limited to errors in coordinate transformation, data loss, or equipment damage.

## Third-Party Services

OpenTopo can connect to third-party NTRIP casters and map tile services. Use of these services is subject to their respective terms. The developers of OpenTopo are not responsible for the availability, accuracy, or terms of third-party services.

## Changes

These terms may be updated. Changes will be documented in the [CHANGELOG](CHANGELOG.md).
